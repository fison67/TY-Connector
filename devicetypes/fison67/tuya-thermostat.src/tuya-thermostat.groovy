/**
 *  Tuya Thermostat(v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2020 fison67@nate.com
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
	definition (name: "Tuya Thermostat", namespace: "fison67", author: "fison67", mnmn:"fison67", vid: "9d1f02f1-771a-3f2e-86b6-6063281a9ba3", ocfDeviceType: "oic.d.thermostat") {
		capability "Actuator"
		capability "Temperature Measurement"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Operating State"
        capability "Thermostat Mode"
        capability "Refresh"
	}

	preferences {
        input name: "powerIndex", title:"Power Index" , type: "number", required: false, defaultValue: 1
        input name: "targetTempIndex", title:"Target Temperature Index" , type: "number", required: false, defaultValue: 2
        input name: "temperatureIndex", title:"Temperature Index" , type: "number", required: false, defaultValue: 3
        input name: "childLockIndex", title:"ChildLock Index" , type: "number", required: false, defaultValue: 6
        
	}

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def installed(){
    sendEvent(name: "supportedThermostatModes", value: ["heat", "off"])
	sendEvent(name: "temperature", value: 15, unit: "C")
}

def updated() {
log.debug "updated"
}

def refresh() {
log.debug "refresh"
	sendEvent(name: "thermostatOperatingState", value: "heating")
}

def setInfo(String app_url, String address) {
	log.debug "${app_url}, ${address}"
	state.app_url = app_url
    state.id = address
}

def setStatus(data){
	log.debug data
	def powerStatus = data[_getPowerIndex().toString()]
    if(powerStatus != null){
    	sendEvent(name:"thermostatMode", value: powerStatus ? "heat" : "off" )
    }
    
   	def targetTemperatureStatus = data[_getTargetTempIndex().toString()]
    if(targetTemperatureStatus != null){
    	sendEvent(name:"heatingSetpoint", value: (targetTemperatureStatus/2), unit: "C"  )
    }
    
    def temperature = data[_getTempIndex().toString()]
    if(temperature != null){
        sendEvent(name:"temperature", value: (temperature/2), unit: "C" )
    }
    sendEvent(name:"thermostatOperatingState", value: (targetTemperatureStatus >= temperature ? "heating" : "idle") )
    
}

def setThermostatMode(mode){
	if(mode == "heat"){
    	heat()
    }else if(mode == "off"){
    	off()
    }
}

def heat(){
	log.debug "heat"
    processCommand("power", "on", _getPowerIndex().toString())
}

def off(){
	log.debug "off"
    processCommand("power", "off", _getPowerIndex().toString())
}

def setHeatingSetpoint(setpoint){
    processCommand("targetTemperature", setpoint, _getTargetTempIndex().toString())
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

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
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

def  _getPowerIndex(){
	if(powerIndex){
    	return powerIndex
    }
    return 1
}

def  _getTargetTempIndex(){
	if(targetTempIndex){
    	return targetTempIndex
    }
    return 2
}

def  _getTempIndex(){
	if(temperatureIndex){
    	return temperatureIndex
    }
    return 3
}

def  _getChildLockIndex(){
	if(childLockIndex){
    	return childLockIndex
    }
    return 6
}


