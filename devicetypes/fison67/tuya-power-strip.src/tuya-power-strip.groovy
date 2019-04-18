/**
 *  Tuya Power Strip (v.0.0.1)
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

metadata {
	definition (name: "Tuya Power Strip", namespace: "fison67", author: "fison67") {
        capability "Switch"		
        capability "Outlet"
        capability "Refresh"
        
        attribute "lastCheckin", "Date"
	}

	simulator { }

	preferences { }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def installChild(data){
    for(def i=0; i<data.count; i++){
        def label = data.name + " #" + (i+1) 
        def componentName = "child-" + (i+1)
		def dni =  ("tuya-connector-" + state.id  + "-" + (i+1))
        def childDevice =  addChildDevice("Tuya Power Strip Child", dni, [completedSetup: true, label: label, componentName: componentName, componentLabel: componentName, isComponent: false])
        childDevice.setID((i+1).toString())
    }
    
    if(data.usb){
    	def label = data.name + " # USB"
        def componentName = "child-usb"
        def childDevice =  addChildDevice("Tuya Power Strip Child", "tuya-connector-" + state.id + "-7" , [completedSetup: true, label: label, componentName: componentName, componentLabel: "child-usb", isComponent: false])
        childDevice.setID("7")
    }
    state.count = data.count
    state.usb = data.usb
}

def setStatus(data){
	log.debug data
	def list = getChildDevices()
    def powerOnCount = 0
    list.each { child ->
        def dni = child.deviceNetworkId
    	def id = dni.split("-")[3]
        if(data[id] != null){
        	child.setStatus("power", data[id])
            if(data[id] == true){
            	powerOnCount++
            }
        }
        
        def leftSecond = data[((id as int) + 8).toString()]
        if(leftSecond != null){
        	child.setStatus("timer", leftSecond)
        }
    }
    
    sendEvent(name: "switch", value: (powerOnCount > 0 ? "on" : "off"))
  
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now, displayed: false)
}

def on(){
	log.debug "on"
    processCommand("power", "on", "0")
}

def off(){
	log.debug "off"
    processCommand("power", "off", "0")
}

def childOn(data){
	log.debug "child On >> ${data}"
    processCommand("power", "on", data)
}

def childOff(data){
	log.debug "child Off >> ${data}"
    processCommand("power", "off", data)
}

def childTimer(data, second){
	log.debug "child Timer >> ${data} >> ${second} second"
    processCommand("timer", second, data)
}

def processCommand(cmd, data, idx){
	def body = [
        "id": state.id,
        "cmd": cmd,
        "data": data,
        "idx": idx
    ]
    def options = makeCommand(body)
    log.debug options
    sendCommand(options, null)
}

def callback(hubitat.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def refresh(){}

def updated(){}

def sendCommand(options, _callback){
	def myhubAction = new hubitat.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/api/control",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}
