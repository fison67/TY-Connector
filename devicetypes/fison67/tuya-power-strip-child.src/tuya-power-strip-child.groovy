/**
 *  Tuya Power Strip Child (v.0.0.1)
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
	definition (name: "Tuya Power Strip Child", namespace: "fison67", author: "fison67") {
        capability "Switch"			
        attribute "lastCheckin", "Date"
        command "setTimer", ["number"]
        command "stop"
	}

	simulator { }

	preferences { }
    
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setID(id){
	state.id = id
}

def setStatus(type, data){
	log.debug "Type[${type}], data >> ${data}"
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now, displayed: false)
    
    switch(type){
    case "power":
        if(device.currentValue("switch") != (data ? "on" : "off")){
            sendEvent(name: (data ? "lastOn" : "lastOff"), value: now, displayed: false )
        }
        sendEvent(name: "switch", value: data ? "on" : "off")
    	break
    case "timer":
    	def timeStr = msToTime(data)
    	sendEvent(name:"leftTime", value: "${timeStr}", displayed: false)
    	sendEvent(name:"time", value: Math.round(data/60), displayed: false)
    	break
    }
}

def getID(){
	return state.id
}

def on(){
	parent.childOn(state.id)
}

def off(){
	parent.childOff(state.id)
}

def stop(){
	parent.childTimer(((state.id as int) + 8).toString(), 0)
}

def setTimer(second){
	parent.childTimer(((state.id as int) + 8).toString(), second * 60)
}

def msToTime(duration) {
    def seconds = (duration%60).intValue()
    def minutes = ((duration/60).intValue() % 60).intValue()
    def hours = ( (duration/(60*60)).intValue() %24).intValue()

    hours = (hours < 10) ? "0" + hours : hours
    minutes = (minutes < 10) ? "0" + minutes : minutes
    seconds = (seconds < 10) ? "0" + seconds : seconds

    return hours + ":" + minutes + ":" + seconds
}
