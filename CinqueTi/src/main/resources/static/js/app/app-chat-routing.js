app.config(function ($routeProvider,$compileProvider) {
    $routeProvider
        .when('/:topic', {
            templateUrl:'/chatPage.html',
            controller: 'chatCtrl'

        });
    $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|file|ftp|blob):|data:image\//);
    // configure html5 to get links working on jsfiddle
    //$locationProvider.html5Mode(true);
});