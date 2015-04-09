
var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec');


var Keyboard = function() {
};

Keyboard.hideKeyboardAccessoryBar = function(hide) {
    exec(null, null, "Keyboard", "hideKeyboardAccessoryBar", [hide]);
};

Keyboard.close = function() {	
    exec(null, null, "Keyboard", "close", []);
};

Keyboard.show = function() {
    exec(null, null, "Keyboard", "show", []);
};

Keyboard.disableScroll = function(disable) {
    exec(null, null, "Keyboard", "disableScroll", [disable]);
};
Keyboard.setFullScreenPreference = function (option, successCallback, errorCallback) {
	exec(successCallback, errorCallback, "Keyboard", "setFullScreenPreference", [option]);
};
Keyboard.getFullScreenPreference = function (successCallback, errorCallback) {
	exec(successCallback, errorCallback, "Keyboard", "getFullScreenPreference", []);
}

/*
Keyboard.styleDark = function(dark) {
 exec(null, null, "Keyboard", "styleDark", [dark]);
};
*/

Keyboard.isVisible = false;

module.exports = Keyboard;



