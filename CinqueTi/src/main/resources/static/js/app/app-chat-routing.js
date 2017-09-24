app.config(function ($routeProvider,$locationProvider) {
    $routeProvider
        .when('/:topic', {
            templateUrl:'/chatPage.html',
            controller: 'chatCtrl'

        })
    // configure html5 to get links working on jsfiddle
    //$locationProvider.html5Mode(true);
});