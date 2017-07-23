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

app.controller('chatCtrl', ['$scope', '$location', '$interval', 'ChatSocket', '$routeParams', 'AddressResolver',
	function($scope, $location, $interval, chatSocket,$routeParams, AddressResolver) {
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

        $scope.alerts       = [];
        $scope.alertTypes   = ['cantiere', 'incidente', 'incendio', 'altro']; //TODO move on server side?
        $scope.topic = $routeParams.topic;
        // alert input monitor variables
        $scope.newAlert     = {
            searchOff: false,
            alert: {},
            reset: function() {this.searchOff = false; this.alert = {};}
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
                        // TODO remove only the tag
                        $scope.alertRef.reset();
                        $scope.newMessage = '';
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
                $scope.newAlert.searchOff = true;
                AddressResolver.query({location: location}, function(addresses) {
                    //TODO turin results search done by server
                    //TODO multiple results
                    var address = addresses[0];

                    $scope.newAlert.alert.lat = address.lat;
                    $scope.newAlert.alert.lng = address.lon;
                    $scope.newAlert.alert.address = address.display_name;
                    $scope.newAlert.alert.type = 'altro';

                    // update text area with alert.address
                    $scope.newMessage = textValue.replace(/\[.*?\]/g, "[" + address.display_name + "]");
                    showAlertTypeSelector();
                } //TODO , function() { error! }
                );

            }
        };

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
                        $scope.alerts[alert.id] = alert;
                        $scope.markers.push({
                            getMessageScope: function() {return $scope; },
                            icon: {
                                iconUrl: '../assets/leaf-orange.png',
                                shadowUrl: '../assets/leaf-shadow.png',
                                iconSize:     [19, 47],
                                shadowSize:   [25, 32]},
                            lat: alert.lat,
                            lng: alert.lng,
                            message: '<chat-alert alert="alerts[\'' + alert.id +'\']" quote="quote(id)" submit="sendRate(id,rate)"></chat-alert>'
                        });
                    });
                });

                /* subscribing to the alerts */
                chatSocket.subscribe('/topic/chat/alerts', function (alertMessage) {
                    var alert = (JSON.parse(alertMessage.body));
                    $scope.alerts[alert.id] = alert;
                    $scope.markers.push({
                        getMessageScope: function() {return $scope; },
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

function showAlertTypeSelector() {
    $('#alertsModal').modal({
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
function printDateTime(timestamp){
    var date = new Date(timestamp);

    var day = date.getDate();

    var month = date.getMonth() + 1;

    var year = date.getFullYear();

    var hours = date.getHours();

    var minutes = "0" + date.getMinutes();

    var seconds = "0" + date.getSeconds();

    var formattedTime = day + "." + month + "." + year + " " + hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);

    return formattedTime;
}

app.directive('chatMessage', function($compile, $timeout) {

	var sent = 		'<li class="left clearfix admin_chat"><span class="chat-img1 pull-right"><img src="https://scontent-mxp1-1.xx.fbcdn.net/v/t1.0-9/995179_496393017119212_1942402182_n.jpg?oh=931db49c4f4f7c905efde31e3371f592&oe=59E4426F" alt="User Avatar" class="img-circle"/></span><div class="chat-body2 clearfix"><p>'+
					 	'{{message.message}}'+
					 	'</p><div class="chat_time pull-left">{{message.date}}</div></div></li>';
	  
	var received = 	'<li class="left clearfix"><span class="chat-img1 pull-left"><img src="http://icons.iconarchive.com/icons/custom-icon-design/pretty-office-8/128/User-blue-icon.png" alt="User Avatar" class="img-circle"/></span>'+
					    '<div class="chat-nickname">'+
					    '{{message.nickname}}'+
					    '</div><div class="chat-body1 clearfix"><p>'+
					    '{{message.message}}'+
					    '</p><div class="chat_time pull-right">{{message.date}}</div></div></li>';
	  
	return {
	    restrict: 'EA',
	    scope: {
	    	message: '=message'
	    },
	    replace: 'true',
	    translucde: 'true',
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
        + "attivo dal " + "{{printDateTime(alert.timestamp)}}" + "<br>"
        + "segnalato da " + "{{alert.nickname}}" + "<br>"
        + "valutazione " + "{{alert.rates}}"
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
        +"<br></div>"

    };

});

app.factory('AddressResolver', ['$resource', function ($resource) {
    return $resource('https://nominatim.openstreetmap.org/search?q=:location,torino&format=json');
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