/**
 *  TY Connector (v.0.0.6)
 *
 * MIT License
 *
 * Copyright (c) 2019 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
 
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

definition(
    name: "TY Connector",
    namespace: "fison67",
    author: "fison67",
    description: "A Connector between Tuya and ST",
    category: "My Apps",
    iconUrl: "https://apprecs.org/gp/images/app-icons/300/85/com.tuya.smart.jpg",
    iconX2Url: "https://apprecs.org/gp/images/app-icons/300/85/com.tuya.smart.jpg",
    iconX3Url: "https://apprecs.org/gp/images/app-icons/300/85/com.tuya.smart.jpg",
    oauth: true
)

preferences {
   page(name: "mainPage")
   page(name: "monitorPage")
   page(name: "langPage")
}


def mainPage() {
	 dynamicPage(name: "mainPage", title: "Tuya Connector", nextPage: null, uninstall: true, install: true) {
   		section("Request New Devices"){
        	input "address", "text", title: "Server address", required: true, description:"IP:Port. ex)192.168.0.100:30040"
        	href url:"http://${settings.address}", style:"embedded", required:false, title:"Local Management", description:"This makes you easy to setup"
        }
        
       	section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
       	}
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    
    if (!state.accessToken) {
        createAccessToken()
    }
    
	state.dniHeaderStr = "ty-connector-"
    
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    initialize()
    setAPIAddress()
}

/**
* deviceNetworkID : Reference Device. Not Remote Device
*/
def getDeviceToNotifyList(deviceNetworkID){
	def list = []
	state.monitorMap.each{ targetNetworkID, _data -> 
        if(deviceNetworkID == _data.id){
        	def item = [:]
            item['id'] = state.dniHeaderStr + targetNetworkID
            item['data'] = _data.data
            list.push(item)
        }
    }
    return list
}

def setAPIAddress(){
	def list = getChildDevices()
    list.each { child ->
        try{
            child.setAddress(settings.address)
        }catch(e){
        }
    }
}

def initialize() {
	log.debug "initialize"
    
    def options = [
     	"method": "POST",
        "path": "/settings/api/smartthings",
        "headers": [
        	"HOST": settings.address,
            "Content-Type": "application/json"
        ],
        "body":[
            "app_url":"${apiServerUrl}/api/smartapps/installations/",
            "app_id":app.id,
            "access_token":state.accessToken
        ]
    ]
    
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: null])
    sendHubCommand(myhubAction)
}

def dataCallback(physicalgraph.device.HubResponse hubResponse) {
    def msg, json, status
    try {
        msg = parseLanMessage(hubResponse.description)
        status = msg.status
        json = msg.json
        log.debug "${json}"
    } catch (e) {
        logger('warn', "Exception caught while parsing data: "+e);
    }
}

def getDataList(){
    def options = [
     	"method": "GET",
        "path": "/requestDevice",
        "headers": [
        	"HOST": settings.address,
            "Content-Type": "application/json"
        ]
    ]
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: dataCallback])
    sendHubCommand(myhubAction)
}

def addDevice(){
	def data = request.JSON
    log.debug data
	def id = data.id
    def name = data.name
    def type = data.type
    def result = [result: 'success']

    def dni = state.dniHeaderStr + id.toLowerCase()
    log.debug("Try >> ADD Tuya Device id=${id} name=${name}, dni=${dni}")
    
    def list = getChildDevices();
    def existDevice = false;
    list.each { child ->
        def _dni = child.deviceNetworkId
        if(_dni == dni){
        	existDevice = true
        }
    }
    
    if(!existDevice){
        def dth = ""; 
        switch(type){
        case "power-strip":
        	dth = "Tuya Power Strip"
        	break
        case "plug-default":
        	dth = "Tuya Plug"
        	break
        case "plug-color":
        	dth = "Tuya Plug Color"
        	break
        case "switch":
        	dth = "Tuya Switch"
        	break
        case "color-light":
        	dth = "Tuya Color Light"
        	break
        case "curtain":
        	dth = "Tuya Curtain"
        	break
        case "thermostat":
        	dth = "Tuya Thermostat"
        	break
        }
        try{
            def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
                "label": name
            ])    
            childDevice.setInfo(settings.address, id)
            childDevice.installChild(data)
        }catch(err){
        	log.error err
            result = [result: 'fail']
        }
        log.debug "Success >> ADD Device DNI=${dni} ${name}"
    }else{
    	result = [result: 'exist']
    }
    
    def resultString = new groovy.json.JsonOutput().toJson(result)
    render contentType: "application/javascript", data: resultString
}

def updateDevice(){
	def data = request.JSON
    log.debug data
    def id = data.id
    def dni = state.dniHeaderStr + id.toLowerCase()
    def chlid = getChildDevice(dni)
    if(chlid){
		chlid.setStatus(data.data)
    }
    def resultString = new groovy.json.JsonOutput().toJson("result":true)
    render contentType: "application/javascript", data: resultString
}

def getDeviceList(){
	def list = getChildDevices();
    def resultList = [];
    list.each { child ->
        def dni = child.deviceNetworkId
        resultList.push( dni.substring(13, dni.length()) );
    }
    
    def configString = new groovy.json.JsonOutput().toJson("list":resultList)
    render contentType: "application/javascript", data: configString
}

def authError() {
    [error: "Permission denied"]
}

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description: "Tuya Connector API",
        platforms: [
            [
                platform: "SmartThings Tuya Connector",
                name: "Tuya Connector",
                app_url: apiServerUrl("/api/smartapps/installations/"),
                app_id: app.id,
                access_token:  state.accessToken
            ]
        ],
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/config")                         { action: [GET: "authError"] }
        path("/list")                         	{ action: [GET: "authError"]  }
        path("/update")                         { action: [POST: "authError"]  }
        path("/add")                         	{ action: [POST: "authError"]  }

    } else {
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/list")                         	{ action: [GET: "getDeviceList"]  }
        path("/update")                         { action: [POST: "updateDevice"]  }
        path("/add")                         	{ action: [POST: "addDevice"]  }
    }
}
