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
    }
]);