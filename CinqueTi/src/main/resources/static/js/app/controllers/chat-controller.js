app.controller('chatCtrl', ['$scope', '$location', '$interval', 'ChatSocket', '$routeParams', 'AddressResolver', 'ToolsResolver', '$timeout', '$anchorScroll', 'ProfilePictureResolver','$compile',
    function($scope, $location, $interval, chatSocket,$routeParams, AddressResolver, ToolsResolver, $timeout, $anchorScroll,ProfilePictureResolver,$compile) {

        // ui-leaflet properties setup
        angular.extend($scope, {
            mapcenter: {
                lat: 45.07,
                lng: 7.69,
                zoom: 13
            },
            legend: {
                position: 'bottomright',
                colors: ['#ff9900', '#e0218a', '#cc0000','#4169e1'],
                labels: ['Cantiere','Incidente','Incendio','Altro']
            },
            markers: {},
            paths: {},
            bounds: {},
            defaults: {
                tileLayer: "https://api.mapbox.com/styles/v1/mapbox/streets-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1IjoiZGF2cjA5MTAiLCJhIjoiY2owemk4N2FmMDJ1ZzMzbno3YjZxZDN3YyJ9.eJdGDM0goIVXcFmMrQX8og",
            }
        });
        //$scope.colors = ['#ff9900', '#a6d785', '#cc0000','#3366cc','#00ccff', '#cc33ff'];

        $scope.username     = '';   // current user username
        $scope.participants = [];   // list of active users in chat room
        $scope.messages     = [];   // list of room messages
        $scope.newMessage   = '';   // model mapping for current message
        $scope.addresses    = [];   // addresses query response
        $scope.chosenAddress= {};   // selected alert for current message
        $scope.modalAddress = '';   // model mapping for modal 'Segnala'
        $scope.infoMessage  = '';   // model mapping for error messages
        $scope.alerts       = [];   // list of active alerts
        // retrieve types from server
        $scope.alertTypes   = ToolsResolver.alertTypes;
        // get the current chat room from route
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

        $scope.$watch('messages', function() {
            // call updateChat() after ui render
            $scope.$evalAsync(function() {
                updateChat();
            });
        },true);

        // send user message
        // normal message, message + alert, message + alert ref
        $scope.sendMessage = function() {
            var messageToServer;
            // check if: something is ready to send && the message contains something different from whitespace
            // otherwise do nothing
            if (!($scope.newMessage.length > 0 && $scope.newMessage.match(/^\s+$/) === null))
                return;
            if ($scope.alertRef.quoting === false)
            {
                // new message, eventually with alert
                messageToServer = JSON.stringify({
                    'message': $scope.newMessage,
                    'lat': $scope.newAlert.alert.lat,
                    'lng': $scope.newAlert.alert.lng,
                    'address': $scope.newAlert.alert.address,
                    'type':$scope.newAlert.alert.type,
                    'quote': false
                });
                // reset to next input
                $scope.newAlert.reset();
            }
            else {
                // quote message
                messageToServer = JSON.stringify({
                    'message': $scope.newMessage,
                    'alertId': $scope.alertRef.id,
                    'quote': true
                });
                $scope.alertRef.reset();
            }
            // send to server the built msg
            chatSocket.send(
                "/app/chat",
                {},
                messageToServer
            );
            $scope.newMessage = '';
        };

        // send the 'update' message to server
        // used when a popup is open by the user
        $scope.sendAlertUpdate = function(id) {
            if (id === null || id === undefined)
                return;
            var messageToServer = JSON.stringify({
                'alertId': id,
                'type':'update'
            });
            chatSocket.send(
                "/app/chat",
                {},
                messageToServer
            );
        };

        // react to user input
        // mapped on ng-change property of input textarea
        $scope.monitorTextInput = function () {
            // get input value
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
                    $scope.newMessage = '';
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

        // react to click on marker
        $scope.$on('leafletDirectiveMarker.chat-map.click', function (e, args) {
            $scope.sendAlertUpdate(args.model.id);
        });

        // called by alertsModal button
        // process user alert selection
        $scope.chooseAlertInfo = function () {
            // build the alert details
            var address = $scope.chosenAddress;
            $scope.newAlert.alert.lat = address.attributes.Y;
            $scope.newAlert.alert.lng = address.attributes.X;
            $scope.newAlert.alert.address = address.address;

            // update text area with alert.address
            // if the input type is "keyboard" we have to replace only the user address between "[" "]"
            // if the input type is "modal" we have to concatenate the "[" "address" "]"
            if ($scope.newAlert.inputType === "keyboard")
                $scope.newMessage = $scope.newMessage.replace(/\[.*?\]/g, "[" + address.address + "]");
            else
                $scope.newMessage = $scope.newMessage + "[" + address.address + "] ";
        }

        // used by 'Segnala' btn (alertBtn)
        $scope.showAddressModal = function () {

            if ($scope.newAlert.searchOff == true)
            {
                $scope.infoMessage = 'Hai già inserito una segnalazione nel messaggio. Per cambiarla rimuovi il tag';
                $timeout(function () { $scope.infoMessage = '';},5000);
                return;
            }
            showAlertAddressSelector();
        }

        $scope.focusOnElement = function(elementId) {
            $location.hash(elementId);
            $anchorScroll();

        }

        // connect alert tagModal with alertsModal
        $scope.processModalAlert = function () {
            if ($scope.modalAddress.length == 0)
                return;
            $scope.buildAlertRequestFromAddress($scope.modalAddress, "modal");
            $scope.modalAddress = '';
        }

        // create alert starting from query string
        $scope.buildAlertRequestFromAddress = function(queryAddress, inputType) {
            $scope.newAlert.searchOff = true; // disable user search
            $scope.newAlert.inputType = inputType; // used for replacing / insert tag into the user message
            // query server and show results to user (when promise is resolved)
            AddressResolver.query({location: queryAddress}, function(addresses) {
                    if (addresses.length === 0)
                    {
                        // no result found
                        // show error message and reset
                        $scope.infoMessage = 'Oh no! Non siamo riusciti a trovare nulla che assomigliasse all\'indirizzo \"' + queryAddress + '\". Controlla di averlo digitato correttamente.' +
                            ' Inoltre ti ricordiamo che il servizio è disponibile solo nei comuni coperti dall\'azienda GTT.';
                        $scope.newMessage = '';
                        $scope.newAlert.reset();
                        $timeout(function () { $scope.infoMessage = '';},10000);
                        return;
                    }
                    // load results to the model
                    $scope.addresses = addresses;
                    // set default alert types
                    $scope.newAlert.alert.type = 'altro';
                    $scope.chosenAddress = addresses[0]; // set the first result as the default one
                    // switch to alertsModal
                    showAlertTypeSelector();
                }, function() {
                    // promise rejected
                    $scope.infoMessage = 'Oh no! Non siamo riusciti a trovare nulla che assomigliasse all\'indirizzo \"' + queryAddress + '\". Controlla di averlo digitato correttamente.' +
                        ' Inoltre ti ricordiamo che il servizio è disponibile solo nei comuni coperti dall\'azienda GTT.';
                    $scope.newMessage = '';
                    $scope.newAlert.reset();
                }
            );
        };

        // let's make a passed marker blinking
        $scope.markerBlinking = function(marker, i) {
            if(marker === null || marker === undefined || i<0 || i>1)
                return;
            if (marker.opacity === 0.5)
                marker.opacity = 1;
            else
                marker.opacity = 0.5

            i--;
            if (i != -1)
                $timeout(function () {
                    $scope.markerBlinking(marker, i);
                }, 500)
        };

        // OPS
        $scope.dancing = function() {
            for (var i in $scope.markers)
            {

                (function(i) {
                    var ops = i % 2;
                    if (ops == 0)
                    {
                        $scope.markers[i].opacity = 0.5;
                        $scope.markerBlinking($scope.markers[i],30);
                    }
                    else
                        $scope.markerBlinking($scope.markers[i],29);

                })(i);
            }
        };

        // center map on a specific alert
        $scope.centerMapOnAlert = function(id) {
            // retrieve marker info from the list
            var alert = $scope.alerts[id];
            if (alert == null || alert == undefined)
                return; // the alert is expired
            // center map
            $scope.mapcenter.lat = alert.lat;
            $scope.mapcenter.lng = alert.lng;
            $scope.mapcenter.zoom = 15;
            // retrieve the marker mapped on current alert
            var marker = $scope.markers[id];
            // make it blink
            marker.opacity = 0.5;
            $timeout(function () {
                $scope.markerBlinking(marker, 10);
            },500);
            //$scope.focusOnElement('navbar-id');
        };

        // process 'Citami' request
        $scope.quote = function (id) {
            if ($scope.newAlert.searchOff === true)
            {
                // a new alert was previously given! Simply erase everything
                $scope.newAlert.reset();
                $scope.newMessage = ''; // we can't remove only the tag, the previous sentence could be wrong
            }
            // disable new alert request
            $scope.newAlert.searchOff = true;
            var ref = $scope.alerts[id];
            // append to textArea the quote
            $scope.newMessage += "[" + ref.address + "]";
            $scope.alertRef.quoting = true;
            $scope.alertRef.id = id;
        };

        // send rate of an alert
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
            // update local info processing old datas
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

        $scope.getPartecipantByNickname = function (nickname) {
            var res = $scope.participants.filter(function (p) { return (p.nickname === nickname);  })
            return res[0];
        };

        // react to return keypress
        $scope.enterKeyListener = function(keyEvent) {
            if (keyEvent.which === 13)
            {
                $scope.sendMessage();
                keyEvent.preventDefault();
            }
        };


        $scope.cancelModalAlert = function(){

            $('#tagModal').modal('hide');
        };

        $scope.cancelAlertModal = function(){

            $scope.newAlert.reset();

            $('#alertsModal').modal('hide');

            $scope.newMessage = $scope.newMessage.replace(/\[.*\]/,'');
        }

        // STOMP setup
        var initStompClient = function() {
            chatSocket.init('/transportsChat');

            chatSocket.connect(function(frame) {

                $scope.username = frame.headers['user-name'];

                chatSocket.subscribe("/topic/presence/" + $scope.topic , function(message) {
                    var updatedList = JSON.parse(message.body).users; // list of current active users

                    if (updatedList.length > $scope.participants.length)
                    {   // new user joined!
                        // extract the list of new users
                        // op: joined = updatedList - partecipants
                        var joined = updatedList.filter(function(el) {
                            return $scope.participants.map(function (obj) { return obj.nickname;  }).indexOf(el) === -1;
                        });
                        // for each new user download the picture and add an object to partecipants list
                        for (var i in joined)
                        {
                            (function(i) {
                                ProfilePictureResolver.getPicture(joined[i]).then(function (image) {
                                    $scope.participants.push({
                                        nickname: joined[i],
                                        picture: _arrayBufferToBase64(image.data)
                                    });
                                });
                            })(i);
                        }
                    }
                    else
                    {
                        // a user left the room
                        // op: partecipants = partecipants - updatedList
                        $scope.participants = $scope.participants.filter(function (p) {
                            return updatedList.indexOf(p.nickname) !== -1;
                        })
                    }
                });

                /* subscribing to the chat topic */
                chatSocket.subscribe('/topic/chat/' + $scope.topic , function(mess) {
                    // new message
                    var message = (JSON.parse(mess.body));
                    var date = new Date(message.date);
                    message.date = isToday(date) ? date.toLocaleTimeString() : date.toLocaleString();
                    // push to messages list
                    $scope.messages.push(message);
                });

                /* to retrieve last messages */
                chatSocket.subscribe('/user/queue/' + $scope.topic , function(messArr) {
                    // 10 most recent retrieved
                    var messageA = (JSON.parse(messArr.body));
                    // process each message
                    messageA.forEach(function(message) {
                        var date = new Date(message.date);
                        message.date = isToday(date) ? date.toLocaleTimeString() : date.toLocaleString();
                        $scope.messages.push(message);
                    });
                });

                /* to retrieve alerts */
                chatSocket.subscribe('/user/queue/alerts', function (alertsArr) {
                    var alertsArray = JSON.parse(alertsArr.body);
                    // process each alert
                    alertsArray.forEach(function(alert) {
                        alert.rates.avg = printRates(alert.rates);
                        $scope.alerts[alert.id] = alert;
                        // extract the icon name starting from the alert type position in alertTypes array
                        var alertIcon = "" + $scope.alertTypes.indexOf(alert.type) + ".png";
                        // add marker to map
                        $scope.markers[alert.id] = {
                            id: alert.id,
                            getMessageScope: function() {return $scope; },
                            icon: {
                                iconUrl: '../assets/alert-markers/' + alertIcon,
                                iconSize:     [32, 32],
                                shadowSize:   [0, 0]},
                            lat: alert.lat,
                            lng: alert.lng,
                            message: '<chat-alert alert="alerts[\'' + alert.id +'\']" quote="quote(id)" submit="sendRate(id,rate)"></chat-alert>'
                        };
                    });
                });

                /* subscribing to the alerts */
                chatSocket.subscribe('/topic/chat/alerts', function (alertMessage) {
                    var alert = (JSON.parse(alertMessage.body));
                    // new alert!
                    if (alert.type === 'remove')
                    {
                        //an alert is expired! Remove it and show a popup to the user
                        var expired = $scope.alerts[alert.id];
                        if (expired == null || expired == undefined)
                            return;
                        $scope.infoMessage = "Ops, la segnalazione è scaduta!";
                        $scope.alerts.splice(alert.id,1);
                        $scope.markers = $scope.markers.filter(function(marker){return (marker.id !== alert.id)});
                        $timeout(function(){$scope.infoMessage = '';}, 5000);
                        return;
                    }
                    alert.rates.avg = printRates(alert.rates);
                    $scope.alerts[alert.id] = alert;
                    //extract the icon name starting from the alert type position in alertTypes array
                    var alertIcon = "" + $scope.alertTypes.indexOf(alert.type) + ".png";
                    $scope.markers[alert.id] = {
                        id: alert.id,
                        getMessageScope: function() {return $scope; },
                        icon: {
                            iconUrl: '../assets/alert-markers/' + alertIcon,
                            iconSize:     [32, 32],
                            shadowSize:   [0, 0]},
                        lat: alert.lat,
                        lng: alert.lng,
                        message: '<chat-alert alert="alerts[\'' + alert.id +'\']" quote="quote(id)" submit="sendRate(id,rate)"></chat-alert>'
                    };
                } );

                /* sending user join message */
                chatSocket.send( "/app/join", {}, JSON.stringify({'name': $scope.username,'topicName': $scope.topic}) );

            }, function(error) {

                $scope.infoMessage = 'Qualcosa è andato storto :(';
                $timeout(function(){$scope.infoMessage = '';}, 5000);
            });
        };

        initStompClient();

        $scope.$on("$locationChangeStart", function () {
            // closing websocket
            chatSocket.close();
        });
    }]);