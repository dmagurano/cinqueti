var app = angular.module('App', ['ngRoute','ui-leaflet', 'ngResource']);

app.config(function ($routeProvider,$compileProvider) {
    $routeProvider
        .when('/:topic', {
            templateUrl:'/chatPage.html',
            controller: 'chatCtrl'

        });
        
    $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|file|ftp|blob):|data:image\//);
});

app.controller('HeaderCtrl', [ '$scope', '$location','$window',
    function HeaderCtrl($scope, $location,$window) {
        $scope.isActive = function (viewLocation) {
            return viewLocation === $location.path();
        };
    }
]);
