var app = angular.module('App', ['ngRoute','ui-leaflet', 'ngResource'])

app.config(function ($routeProvider,$locationProvider) {
	  $routeProvider
      .when('/:topic', {
          templateUrl:'/chatPage.html',
          controller: 'chatCtrl'
          
      })
    // configure html5 to get links working on jsfiddle
    //$locationProvider.html5Mode(true);
});

app.controller('HeaderCtrl', [ '$scope', '$location',
	function HeaderCtrl($scope, $location) { 
	    $scope.isActive = function (viewLocation) { 
	    	 return viewLocation === $location.path();
	    };
	    
	    $scope.chatOpen = function (topic) {
            $window.location.href = "/chat/#!/" + topic;
        }
}]);

app.controller('chatCtrl', ['$scope', '$location', '$interval', 'ChatSocket', '$routeParams', 'AddressResolver', 'ToolsResolver', '$window',
	function($scope, $location, $interval, chatSocket,$routeParams, AddressResolver, ToolsResolver) {
        angular.extend($scope, {
            turin: {
                lat: 45.07,
                lng: 7.69,
                zoom: 13
            },
            markers: [],
            paths: {},
            bounds: {},
            defaults: {
                tileLayer: "https://api.mapbox.com/styles/v1/mapbox/streets-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1IjoiZGF2cjA5MTAiLCJhIjoiY2owemk4N2FmMDJ1ZzMzbno3YjZxZDN3YyJ9.eJdGDM0goIVXcFmMrQX8og",
            }
        });

        $scope.username     = '';
        $scope.participants = [];
        $scope.messages     = [];
        $scope.newMessage   = '';
        $scope.addresses    = [];
        $scope.chosenAddress= {};
        $scope.modalAddress = '';
        $scope.alerts       = [];
        //$scope.alertTypes   = ['cantiere', 'incidente', 'incendio', 'altro']; //TODO move on server side?
        $scope.alertTypes   = ToolsResolver.alertTypes;
        $scope.topic = $routeParams.topic;
        // alert input monitor variables
        $scope.newAlert     = {
            searchOff: false,
            inputType: '',
            alert: {},
            reset: function() {
                this.searchOff = false; this.alert = {}; this.inputType = '';
                $scope.chosenAddress = {}; $scope.addresses = [];
            }
        };
        // alert reference variables
        $scope.alertRef     = {
            quoting: false,
            id: null,
            reset: function() {this.quoting = false; this.id = null; $scope.newAlert.searchOff = false;}
        };
//        $scope.$watch(function(){
//            return $location.hash();
//          },
//          function(topic){
//            $scope.topic = topic;
//          }
//        );
         
        $scope.$watch('messages', function() {
        //serve affinche venga chiamata la funzione dopo il
        //render dell'interfaccia
    	   $scope.$evalAsync(function() {
           		updateChat();
           });
        },true);
        /*
        $scope.$watch('alerts', function() {
            //serve affinche venga chiamata la funzione dopo il
            //render dell'interfaccia
            $scope.$evalAsync(function() {
                updateChat();
            });
        },true);
        */
        
        $scope.sendMessage = function() {
        	var messageToServer;
            // check if: something is ready to send && the message contains something different from whitespace
            // otherwise do nothing
            if (!($scope.newMessage.length > 0 && $scope.newMessage.match(/^\s+$/) === null))
                return;
        	if ($scope.alertRef.quoting === false)
            {
                messageToServer = JSON.stringify({
                    'message': $scope.newMessage,
                    'lat': $scope.newAlert.alert.lat,
                    'lng': $scope.newAlert.alert.lng,
                    'address': $scope.newAlert.alert.address,
                    'type':$scope.newAlert.alert.type
                });
                $scope.newAlert.reset();
            }
            else {
                messageToServer = JSON.stringify({
                    'message': $scope.newMessage,
                    'alertId': $scope.alertRef.id
                });
                $scope.alertRef.reset();
            }
        	chatSocket.send( 
        			"/app/chat", 
        			{},
        			messageToServer
        	);
            $scope.newMessage = '';
            //$("#textArea").focus();
        };

        $scope.monitorTextInput = function () {
            var textValue = $scope.newMessage;
            // check if we already have an alert registered
            if($scope.newAlert.searchOff === true){
                // check if something change: do we still have the tag into the msg?
                if (textValue.indexOf("[") !== -1
                    && textValue.indexOf("]")  !== -1 )
                    return; // ok, done
                else{
                    // the user removes the tag, let's clear the old value
                    // note: we can have a tag given by a reference or a tag given by a new alert
                    if ($scope.alertRef.quoting)
                    {
                        // first case: the user attempt to remove the reference
                        $scope.alertRef.reset();
                    }
                    else
                    {
                        // second case: the user attempt to remove the alert
                        $scope.newAlert.reset();
                    }
                    return;
                }
            }
            // check if the user has entered a new alert
            var matches = textValue.match(/\[(.*?)\]/);
            if (matches) {
                // new alert found!
                var location = matches[1]; //get the tag string
                $scope.buildAlertRequestFromAddress(location, "keyboard");
            }
        };
        
        $scope.chooseAlertInfo = function () {
            var address = $scope.chosenAddress;
            $scope.newAlert.alert.lat = address.lat;
            $scope.newAlert.alert.lng = address.lon;
            $scope.newAlert.alert.address = address.display_name;

            // update text area with alert.address
            // if the input type is "keyboard" we have to replace only the user address between "[" "]"
            // if the input type is "modal" we have to concatenate the "[" "address" "]"
            if ($scope.newAlert.inputType === "keyboard")
                $scope.newMessage = $scope.newMessage.replace(/\[.*?\]/g, "[" + address.display_name + "]");
            else
                $scope.newMessage = $scope.newMessage + "[" + address.display_name + "] ";
        }

        $scope.showAddressModal = function () {
            // used by 'Segnala' btn (alertBtn)
            showAlertAddressSelector();
        }

        $scope.focusOnHelp = function() {
            $location.hash("suggestionsArea");
            $anchorScroll();
        }

        $scope.processModalAlert = function () {
            if ($scope.modalAddress.length == 0)
                return;
            $scope.buildAlertRequestFromAddress($scope.modalAddress, "modal");
            $scope.modalAddress = '';
        }

        $scope.buildAlertRequestFromAddress = function(queryAddress, inputType) {
            var location = queryAddress; //get the tag string
            $scope.newAlert.searchOff = true;
            $scope.newAlert.inputType = inputType; // used for replacing / insert tag into the user message
            AddressResolver.query({location: location}, function(addresses) {
                    $scope.addresses = addresses;
                    $scope.newAlert.alert.type = 'altro';
                    $scope.chosenAddress = addresses[0]; // set the first result as the default one
                    if (addresses.length == 1)
                    {
                        // just one result! Simply call the function in order to show the result to user
                        $scope.chooseAlertInfo();
                    }
                    showAlertTypeSelector();
                }, function() {
                    alert("Siamo spiacenti, non siamo riusciti a risolvere l'indirizzo");
                    $scope.newMessage = '';
                    $scope.newAlert.reset();
                }
            );
        }

        $scope.quote = function (id) {
            if ($scope.newAlert.searchOff === true)
            {
                // a new alert was previously given! Simply erase everything
                $scope.newAlert.reset();
                $scope.newMessage = ''; // we can't remove only the tag, the previous sentence could be wrong
            }
            // disable new alert
            $scope.newAlert.searchOff = true;
            var ref = $scope.alerts[id];
            // append to textArea the quote
            $scope.newMessage += "[" + ref.address + "]";
            $scope.alertRef.quoting = true;
            $scope.alertRef.id = id;
        };
        $scope.sendRate = function(id, rate) {
            var messageToServer;
            messageToServer = JSON.stringify({
                'alertId': id,
                'value': rate
            });
            chatSocket.send(
                "/app/rate",
                {},
                messageToServer
            );
            // update local info
            var rates = $scope.alerts[id].rates
            if ($scope.alerts[id].myRate === 0)
            {
                rates.push({value: rate});
                rates.avg = ((rates.avg*(rates.length-1) + rate)/rates.length).toFixed(2);
            }
            else
                rates.avg = ((rates.avg*(rates.length) - $scope.alerts[id].myRate + rate)/rates.length).toFixed(2);
            $scope.alerts[id].myRate = rate;
        };

        $scope.enterKeyListener = function(keyEvent) {
        	if (keyEvent.which === 13)
        	   $scope.sendMessage();
        };

            
        var initStompClient = function() {
            chatSocket.init('/transportsChat');
            
            chatSocket.connect(function(frame) {
                  
                $scope.username = frame.headers['user-name'];

                chatSocket.subscribe("/topic/presence/" + $scope.topic , function(message) {
                    $scope.participants = JSON.parse(message.body).users;
                });
                  
                /* subscribing to the chat topic */
                chatSocket.subscribe('/topic/chat/' + $scope.topic , function(mess) {
                    var message = (JSON.parse(mess.body)); 
                    var date = new Date(message.date);
                    message.date = isToday(date) ? date.toLocaleTimeString() : date.toLocaleString();
                    $scope.messages.push(message);
                });

                    /* to retrieve last messages */
                chatSocket.subscribe('/user/queue/' + $scope.topic , function(messArr) {
                     var messageA = (JSON.parse(messArr.body));
                     messageA.forEach(function(message) {
                        var date = new Date(message.date);
                        message.date = isToday(date) ? date.toLocaleTimeString() : date.toLocaleString();
                        $scope.messages.push(message);
                    });
                });

                /* to retrieve alerts */
                chatSocket.subscribe('/user/queue/alerts', function (alertsArr) {
                    var alertsArray = JSON.parse(alertsArr.body);
                    alertsArray.forEach(function(alert) {
                        alert.rates.avg = printRates(alert.rates);
                        $scope.alerts[alert.id] = alert;
                        //extract the icon name starting from the alert type position in alertTypes array
                        var alertIcon = "" + $scope.alertTypes.indexOf(alert.type) + ".png"
                        $scope.markers.push({
                            getMessageScope: function() {return $scope; },
                            icon: {
                                iconUrl: '../assets/alert-markers/' + alertIcon,
                                //shadowUrl: '../assets/leaf-shadow.png',
                                iconSize:     [32, 32],
                                shadowSize:   [0, 0]},
                            lat: alert.lat,
                            lng: alert.lng,
                            message: '<chat-alert alert="alerts[\'' + alert.id +'\']" quote="quote(id)" submit="sendRate(id,rate)"></chat-alert>'
                        });
                    });
                });

                /* subscribing to the alerts */
                chatSocket.subscribe('/topic/chat/alerts', function (alertMessage) {
                    var alert = (JSON.parse(alertMessage.body));
                    alert.rates.avg = printRates(alert.rates);
                    $scope.alerts[alert.id] = alert;
                    //extract the icon name starting from the alert type position in alertTypes array
                    var alertIcon = "" + $scope.alertTypes.indexOf(alert.type) + ".png"
                    $scope.markers.push({
                        getMessageScope: function() {return $scope; },
                        icon: {
                            iconUrl: '../assets/alert-markers/' + alertIcon,
                            //shadowUrl: '../assets/leaf-shadow.png',
                            iconSize:     [32, 32],
                            shadowSize:   [0, 0]},
                        lat: alert.lat,
                        lng: alert.lng,
                        message: '<chat-alert alert="alerts[\'' + alert.id +'\']" quote="quote(id)" submit="sendRate(id,rate)"></chat-alert>'
                    });
                } );

                /* sending user join message */
                chatSocket.send( "/app/join", {}, JSON.stringify({'name': $scope.username,'topicName': $scope.topic}) );  
                  
                // chatSocket.subscribe("/user/exchange/amq.direct/errors", function(message) {
                //     toaster.pop('error', "Error", message.body);
                // });
                  
            }, function(error) {
                toaster.pop('error', 'Error', 'Connection error ' + error);
                
            });
        };
          
        initStompClient();
}]);

//function parseMessage(mess){
//    var message = (JSON.parse(mess.body));
//    var date = new Date(message.date);
//    message.date = isToday(date) ? date.toLocaleTimeString() : date.toLocaleString();
//    $scope.messages.unshift(message);
//};

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

app.directive('chatMessage', function($compile, $timeout) {

	var sent = 		'<li class="left clearfix admin_chat"><span class="chat-img1 pull-right"><img src="https://scontent-mxp1-1.xx.fbcdn.net/v/t1.0-9/995179_496393017119212_1942402182_n.jpg?oh=931db49c4f4f7c905efde31e3371f592&oe=59E4426F" alt="User Avatar" class="img-circle"/></span>' +
                    '<div class="chat-body2 clearfix" ng-switch on="message.alertId">' +
                    '<p ng-switch-when="null" ng-bind-html="formatChatMessage(message.message, 1)">'+'</p>'+
                    //'<p ng-switch-default>ok'+'{{formatChatMessage(message.message)}}'+'</p>'+
                    '<p ng-switch-default ng-bind-html="formatChatMessage(message.message, 0)">'+'</p>'+
                    '<div class="chat_time pull-left">{{message.date}}</div></div></li>';
	  
	var received = 	'<li class="left clearfix"><span class="chat-img1 pull-left"><img src="http://icons.iconarchive.com/icons/custom-icon-design/pretty-office-8/128/User-blue-icon.png" alt="User Avatar" class="img-circle"/></span>'+
					'<div class="chat-nickname">'+'{{message.nickname}}'+'</div>' +
                    '<div class="chat-body1 clearfix" ng-switch on="message.alertId">'+
                        '<p ng-switch-when="null" ng-bind-html="formatChatMessage(message.message, 1)">'+'</p>'+
                        '<p ng-switch-default ng-bind-html="formatChatMessage(message.message, 0)">'+'</p>'+
                        '<div class="chat_time pull-right">{{message.date}}</div>' +
                    '</div></li>';
	  
	return {
	    restrict: 'EA',
	    scope: {
	    	message: '=message'
	    },
        controller: function($scope, $sce) {
            $scope.formatChatMessage = function(textMsg, old) {
                if (old == 0)
                    textMsg = textMsg.replace("[", "<span class=\"label label-danger\">");
                else
                    textMsg = textMsg.replace("[", "<span class=\"label label-warning\">");
                textMsg = textMsg.replace("]", "</span>");
                //textMsg = textMsg.replace("[", "<b>");
                //textMsg = textMsg.replace("]", "</b>");

                return $sce.trustAsHtml(textMsg);
            };

        },
	    replace: 'true',
	    //transclude: 'true', //TODO remove this
	    compile: function(tElem, tAttr) {
	    	return function(scope, el, attr, ctrl, trans) {
		        if (scope.message.username != scope.$parent.$parent.username) {
                    var mess = $compile(received)(scope);
		        	el.append(mess);
		        } else {
                    var mess = $compile(sent)(scope);
                    el.append(mess);
		        }
	        }
	    }
    };

});

app.directive('chatAlert', function($compile, $timeout) {
    return {
        restrict: 'EA',
        scope: {
            alert: '=',
            quote: '&',
            submit: '&'
        },
        replace: 'true',
        // TODO add functionalities
        template: "<div><b>" + "{{alert.type.toUpperCase()}}" + "</b>"
        + " <button ng-click=\"quote({id: alert.id})\">Citami</button><br>"
        + "{{alert.address}}" + "<br>"
        + "attivo dal " + "{{printDateTime(alert.recvTimestamp)}}" + "<br>"
        + "segnalato da " + "{{alert.nickname}}" + "<br>"
        + "valutazione " + "{{alert.rates.avg}}"
        + "	<div class=\"stars\" id=\"rating-in-" + "{{alert.id}}" + "\">"
        + "	<form action=\"\">"
        + "			<input class=\"star star-5\" id=\"star-5\" type=\"radio\" name=\"star\" value=\"5\" ng-click=\"submit({id: alert.id,rate: 5})\"/>"
        + "			<label class=\"star star-5\" for=\"star-5\"></label>"
        + "			<input class=\"star star-4\" id=\"star-4\" type=\"radio\" name=\"star\" value=\"4\" ng-click=\"submit({id: alert.id,rate: 4})\"/>"
        + "			<label class=\"star star-4\" for=\"star-4\"></label>"
        + "			<input class=\"star star-3\" id=\"star-3\" type=\"radio\" name=\"star\" value=\"3\" ng-click=\"submit({id: alert.id,rate: 3})\"/>"
        + "			<label class=\"star star-3\" for=\"star-3\"></label>"
        + "			<input class=\"star star-2\" id=\"star-2\" type=\"radio\" name=\"star\" value=\"2\" ng-click=\"submit({id: alert.id,rate: 2})\"/>"
        + "			<label class=\"star star-2\" for=\"star-2\"></label>"
        + "			<input class=\"star star-1\" id=\"star-1\" type=\"radio\" name=\"star\" value=\"1\" ng-click=\"submit({id: alert.id,rate: 1})\"/>"
        + "			<label class=\"star star-1\" for=\"star-1\"></label>"
        + "		</form>"
        + "	</div>"
        +"<br></div>",
        controller: function($scope) {

            $scope.printDateTime = function(timestamp){
                var date = new Date(timestamp);

                var day = date.getDate();

                var month = date.getMonth() + 1;

                var year = date.getFullYear();

                var hours = date.getHours();

                var minutes = "0" + date.getMinutes();

                var seconds = "0" + date.getSeconds();

                var formattedTime = day + "." + month + "." + year + " " + hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);

                return formattedTime;
            };
        }


    };

});


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

app.factory('ChatSocket', ['$rootScope', function($rootScope) {
            var stompClient;
            
            var wrappedSocket = {
                    
                    init: function(url) {
                        stompClient = Stomp.over(new SockJS(url));
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
                    }
            }
            
            return wrappedSocket;
}]);