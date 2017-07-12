app.controller('CalculateController', ['$scope', 'PathsDataProvider', 'leafletBoundsHelpers',
    function ($scope, PathsDataProvider, leafletBoundsHelpers) {
        angular.extend($scope, {
            turin: {
                        lat: 45.07,
                        lng: 7.69,
                        zoom: 13
            },
            paths: {},
            bounds: {},
            defaults: {
                tileLayer: "https://api.mapbox.com/styles/v1/mapbox/streets-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1IjoiZGF2cjA5MTAiLCJhIjoiY2owemk4N2FmMDJ1ZzMzbno3YjZxZDN3YyJ9.eJdGDM0goIVXcFmMrQX8og",
            }
        });

        $scope.pathDetailsVisibility = true;
        $scope.pathDetails = PathsDataProvider.getPathDetails();
        this.calculate = function(){
            PathsDataProvider.setSource(this.source);
            PathsDataProvider.setDestination(this.destination);

            var PathInfo = PathsDataProvider.getPath();

            var polylines = PathInfo.polylines;
            
            $scope.paths = {};
            for (var i = 0; i < polylines.length; i++)
                $scope.paths['line' + i] = polylines[i];

            $scope.bounds = leafletBoundsHelpers.createBoundsFromArray(PathInfo.bounds);
            $scope.pathDetailsVisibility = false;
        }


    }
]);

app.factory('PathsDataProvider', [ 'Percorsi',
    function (percorsi) {
        var source, destination;
        var currentPath = [];

        return {
            setSource: function(src){
                source = src;
            },
            setDestination: function(dst){
                destination = dst;
            },
            getPathDetails : function() {
                return currentPath;
            },
            getPath: function() {
                // FUTURE use source and destination

                var PathInfo = new Object();

                var max_lat,max_lng,min_lat,min_lng;
                min_lat = 90; max_lat = -90; min_lng = 180; max_lng = -180;

                var firstElement = true;

                // return a random path                
                var i = Math.floor(Math.random() * (percorsi.paths.length));
                // empty the current path array for html details
                currentPath.length = 0;

                var curPolyline = {};
                var previousMode;
                var polylines = [];
                var myline;

                for (var j = 0; j < percorsi.paths[i].length; j++){
                    var edge = percorsi.paths[i][j];

                    var pointSrc = new Object();
                    var pointDst = new Object();

                    if (firstElement){                        
                        previousMode = edge.mode;
                        firstElement = false;

                        curPolyline = createPolyline(edge.mode);
                    }
                    else{
                        if ( previousMode != edge.mode){
                            polylines.push(curPolyline);

                            previousMode = edge.mode;

                            curPolyline = createPolyline(edge.mode);
                        }
                    }

                    pointSrc.lat = edge.latSrc;
                    pointSrc.lng = edge.lonSrc;

                    pointDst.lat = edge.latDst;
                    pointDst.lng = edge.lonDst;

                    curPolyline.latlngs.push(pointSrc);
                    curPolyline.latlngs.push(pointDst);

                    if (pointSrc.lat < min_lat)
                        min_lat = pointSrc.lat;
                    if (pointDst.lat < min_lat)
                        min_lat = pointDst.lat;

                    if (pointSrc.lat > max_lat)
                        max_lat = pointSrc.lat;
                    if (pointDst.lat > max_lat)
                        max_lat = pointDst.lat;

                    if (pointSrc.lng < min_lng)
                        min_lng = pointSrc.lng;
                    if (pointDst.lng < min_lng)
                        min_lng = pointDst.lng;

                    if (pointSrc.lng > max_lng)
                        max_lng = pointSrc.lng;
                    if (pointDst.lng > max_lng)
                        max_lng = pointDst.lng;

                    // now fill the currentPath array in order to show the path details
                    if (myline == undefined)
                        myline = new Object();
                    else if (myline.edgeLine === edge.edgeLine){
                        myline.cost += edge.cost;
                        continue;
                    }
                    else {
                        currentPath.push(myline);
                        myline = new Object();
                    }
                    myline.cost = edge.cost;

                    myline.edgeLine = edge.edgeLine;
                    myline.mode = edge.mode;
                }

                PathInfo.bounds = [
                                    [max_lat, max_lng],
                                    [min_lat, min_lng]
                                ];

                PathInfo.polylines = polylines;

                return PathInfo;
            }
        };
    }
]);

function createPolyline(mode){
    polyline = {
        type: "polyline",
        weight: 6,
        color: '',
        latlngs: []
    };

    if (mode == 0){
        polyline.color = "yellow";
        polyline.dashArray = "";
    }
    else{
        polyline.color = "blue";
        polyline.dashArray = "5, 10";
    }

    return polyline
}