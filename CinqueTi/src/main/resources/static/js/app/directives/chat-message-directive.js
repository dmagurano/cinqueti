app.directive('chatMessage', function($compile) {

    var sent = 		'<li class="left clearfix admin_chat"><span class="chat-img1 pull-right">' +
        '<img ng-if="user.picture !== undefined" ng-src="data:image/JPG;base64,{{user.picture}}" alt="..." class="img-circle"/>' +
        '<img ng-if="user.picture === undefined" ng-src="/rest/users/{{message.nickname}}/image" alt="..." class="img-circle"/>' +
        '</span>' +
        '<div class="chat-body2 clearfix" ng-switch on="message.alertId">' +
        '<p ng-switch-when="null" ng-bind-html="formatChatMessage(message.message, message.alertId, message.quote, 1)">'+'</p>'+
        '<p ng-switch-default ng-click="centerMapOnAlert({id:message.alertId})" ng-bind-html="formatChatMessage(message.message, message.alertId, message.quote, 0)">'+'</p>'
    '<div class="chat_time pull-left">{{message.date}}</div></div></li>';

    var received = 	'<li class="left clearfix"><span class="chat-img1 pull-left">' +
        '<img ng-if="user.picture !== undefined" ng-src="data:image/JPG;base64,{{user.picture}}" alt="..." class="img-circle"/>' +
        '<img ng-if="user.picture === undefined" ng-src="/rest/users/{{message.nickname}}/image" alt="..." class="img-circle"/>' +
        '</span>'+
        '<div class="chat-nickname">'+'{{message.nickname}}'+'</div>' +
        '<div class="chat-body1 clearfix" ng-switch on="message.alertId">'+
        '<p ng-switch-when="null" ng-bind-html="formatChatMessage(message.message, message.alertId, message.quote, 1)">'+'</p>'+
        '<p ng-switch-default ng-click="centerMapOnAlert({id:message.alertId})" ng-bind-html="formatChatMessage(message.message, message.alertId, message.quote, 0)">'+'</p>'+
        '<div class="chat_time pull-right">{{message.date}}</div>' +
        '</div></li>';

    return {
        restrict: 'EA',
        scope: {
            message: '=message',
            centerMapOnAlert: '&centerMapOnAlert',
            user: '=user'
        },
        controller: function($scope, $sce) {
            $scope.formatChatMessage = function(textMsg, id, quoting, old) {
                if (quoting === true)
                    textMsg = textMsg.replace("[", "<span class=\"label label-info with-hand\">");
                else if (old === 0)
                    textMsg = textMsg.replace("[", "<span class=\"label label-danger with-hand\">");
                else
                    textMsg = textMsg.replace("[", "<span class=\"label label-warning\">");
                textMsg = textMsg.replace("]", "</span>");
                return $sce.trustAsHtml(textMsg);
            };

        },
        replace: 'true',
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