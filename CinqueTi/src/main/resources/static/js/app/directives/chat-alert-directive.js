app.directive('chatAlert', function($compile, $timeout) {
    return {
        restrict: 'EA',
        scope: {
            alert: '=',
            quote: '&',
            submit: '&'
        },
        replace: 'true',
        templateUrl: "/directives/alert.html",
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