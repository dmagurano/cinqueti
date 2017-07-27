app.factory('linesCache', ['$cacheFactory', function($cacheFactory) {
        return $cacheFactory('lines-cache');
}]);
