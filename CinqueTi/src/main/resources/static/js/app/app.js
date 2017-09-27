/*
 TODO
 different colour for on foot or by bus (better: different colour for each line)
 update table containing path details
 make edges follow the roads
 */

var app = angular.module('App', ['ngRoute', 'ngResource', 'ui-leaflet'])

app.config(function ($routeProvider, $locationProvider) {
    $routeProvider
        .when('/', {
            templateUrl: 'main.html',
            controller: 'MainCtrl',
            controllerAs: 'ctrl'
        })
        .when('/calculate', {
            templateUrl: 'calculate.html',
            controller: 'CalculateController',
            controllerAs: 'ctrl'
        })
        .when('/lines/:lineID', {
            templateUrl: 'main.html',
            controller: 'MainCtrl',
            controllerAs: 'ctrl'
        })
    .otherwise({ redirectTo: "/" });

});

app.controller('HeaderCtrl', [ '$scope', '$location', '$window',
    function HeaderCtrl($scope, $location, $window) {
        $scope.isActive = function (viewLocation) {
            return viewLocation === $location.path();
        };
        // TODO remove this
        /*
        $scope.chatOpen = function (topic) {
            $window.location.href = "/chat/#!/" + topic;
        }
        */

    }]);