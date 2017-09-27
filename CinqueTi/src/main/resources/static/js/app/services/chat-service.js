function _arrayBufferToBase64( buffer ) {
    var binary = '';
    var bytes = new Uint8Array( buffer );
    var len = bytes.byteLength;
    for (var i = 0; i < len; i++) {
        binary += String.fromCharCode( bytes[ i ] );
    }
    return window.btoa( binary );
}

function updateChat() {
    var chatdiv = $('.chat_area');
    chatdiv.scrollTop(chatdiv.get(0).scrollHeight);
}

function printRates(rates) {
    var sum = 0
    if(rates.length === 0)
        return (0).toFixed(2);
    for (var i = 0; i<rates.length; i++) {
        sum += rates[i].value;
    }
    return (sum/rates.length).toFixed(2);
};

function showAlertTypeSelector() {
    $('#alertsModal').modal({
        backdrop: 'static',
        keyboard: false
    });
}

function showAlertAddressSelector() {
    $('#tagModal').modal({
        backdrop: 'static',
        keyboard: false
    });
}

function isToday (date) {
    var now = new Date();
    if (date.toDateString() == now.toDateString())
        return true;
    else
        return false;
};

app.factory('AddressResolver', ['$resource', function ($resource) {
    //return $resource('https://nominatim.openstreetmap.org/search?q=:location,torino&format=json');
    return $resource('/rest/address?address=:location');
}]);

app.factory('ToolsResolver', ['$resource', function ($resource) {
    var alertTypesRes = $resource('/rest/alerttypes');
    var alertTypes = alertTypesRes.query();
    return {
        alertTypes: alertTypes
    }
}]);

app.factory('ProfilePictureResolver', ['$http', function ($http) {
    return {
        getPicture: function(nickname) {
            return $http.get('/rest/users/' + nickname + '/image', {
                responseType:"arraybuffer"
            });
        }
    }


}]);

app.factory('ChatSocket', ['$rootScope', function($rootScope) {
    var stompClient;
    var sockJS = null;

    var wrappedSocket = {

        init: function(url) {
            sockJS = new SockJS(url);
            stompClient = Stomp.over(sockJS);
        },
        connect: function(successCallback, errorCallback) {

            stompClient.connect({}, function(frame) {
                $rootScope.$apply(function() {
                    successCallback(frame);
                });
            }, function(error) {
                $rootScope.$apply(function(){
                    errorCallback(error);
                });
            });
        },
        subscribe : function(destination, callback) {
            stompClient.subscribe(destination, function(message) {
                $rootScope.$apply(function(){
                    callback(message);
                });
            });
        },
        send: function(destination, headers, object) {
            stompClient.send(destination, headers, object);
        },
        close: function () {
            sockJS.close();
        }
    }

    return wrappedSocket;
}]);
