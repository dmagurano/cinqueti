var app = angular.module('App', ['ngRoute','ui-leaflet'])

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

app.controller('chatCtrl', ['$scope', '$location', '$interval', 'ChatSocket', '$routeParams',
	function($scope, $location, $interval, chatSocket,$routeParams) {
          
        $scope.username     = '';
        $scope.participants = [];
        $scope.messages     = [];
        $scope.newMessage   = ''; 
        $scope.topic = $routeParams.topic;
        
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
        
        $scope.sendMessage = function() {
        	var messageToServer;
        	
        	messageToServer = JSON.stringify({
    			'message': $scope.newMessage, 
    			'alertId': alert.id
    		});
            
        	chatSocket.send( 
        			"/app/chat", 
        			{},
        			messageToServer
        	);
           
            $scope.newMessage = '';
            $("#textArea").focus();
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

function isToday (date) {
    var now = new Date();
    if (date.toDateString() == now.toDateString())
        return true;
    else 
        return false;
};

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