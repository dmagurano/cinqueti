app.controller('MainCtrl', [ '$scope', 'LinesDataProvider', 'leafletBoundsHelpers', '$routeParams', '$location',
    function ($scope, LinesDataProvider, leafletBoundsHelpers, $routeParams, $location) {

        this.lineID = $routeParams.lineID;

        this.lines = LinesDataProvider.load();

        // updates route
        this.goToLink = function(line) {
          $location.path('lines/' + line.line);
        };

        angular.extend($scope, {
            center: {},
            bounds: {},
            paths: {},
            markers: {},
            defaults: {
                tileLayer: "https://api.mapbox.com/styles/v1/mapbox/streets-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1IjoiZGF2cjA5MTAiLCJhIjoiY2owemk4N2FmMDJ1ZzMzbno3YjZxZDN3YyJ9.eJdGDM0goIVXcFmMrQX8og",
            }
        });

        if ($routeParams.lineID) {
            var PathInfo = LinesDataProvider.loadPath($routeParams.lineID);
            $scope.paths = PathInfo.paths;
            $scope.markers = PathInfo.markers;
            $scope.bounds = leafletBoundsHelpers.createBoundsFromArray(PathInfo.bounds);
        } else {
            $scope.center = { lat: 45.07,
                         lng: 7.69,
                         zoom: 13 
                     }
        };

        $scope.$on('leafletDirectiveMarker.click', function(e, args) {
            clickedBusStopId = args.model.id;

            var busStopLines = LinesDataProvider.queryBusStop(clickedBusStopId);

            var busStopLinesHTML = "<br>Other lines:";
            angular.forEach(busStopLines, function(value, key){
                busStopLinesHTML += "<br>"+value;
            });

            var popup = args.leafletObject._popup;

            popup.setContent("Bus stop: <strong>" + $routeParams.lineID + "</strong>" + busStopLinesHTML);
        });
    }
]);

app.factory('LinesDataProvider', ['Linee', '$filter',
    function (linee,$filter) {
        return {
            load : function () { return linee.lines; },
            query : function (id) {
                for (var i=0; i < linee.lines.length; i++ ) {
                    if (linee.lines[i].line == id)
                        return linee.lines[i].stops;
                }
            },
            loadPath : function (id) {
                var PathInfo = new Object();

                var direction = "going";
                var requestedDirection = "all";
                var goingPhase = true;
                var firstElement = true;
                var firstBusId;
                var lastProcessedBusId;

                var max_lat,max_lng,min_lat,min_lng;
                min_lat = 90; max_lat = -90; min_lng = 180; max_lng = -180;

                var paths = {
                    going_path : {
                        type : "polyline",
                        weight: 4,
                        color: 'green',
                        message: "Line: <strong>" + id + "</strong>",
                        latlngs : []
                    },
                    return_path : {
                        type : "polyline",
                        weight: 4,
                        color: 'blue',
                        message: "Line: <strong>" + id + "</strong>",
                        latlngs : []
                    }
                };

                var markers = {};

                this.query(id).forEach( function (stop) {
                    var point = new Object();

                    var stop = $filter('filter')(linee.stops, function (s) {return s.id === stop;})[0];

                    if ( firstElement ){
                        firstBusId = stop.id;
                        lastProcessedBusId = firstBusId;
                        firstElement = false;
                    }
                    else{
                        if (stop.id === lastProcessedBusId)
                            direction = "return";
                        else
                            lastProcessedBusId = stop.id;
                    }

                    point.lat = stop.latLng[0];
                    point.lng = stop.latLng[1];
                    
                    if (requestedDirection === "all"){
                        if (direction === "going"){
                            paths.going_path.latlngs.push(point);
                        }
                        else{
                            paths.return_path.latlngs.push(point);
                        }
                    }
                    else{
                        if (requestedDirection === "going" && direction === "going"){
                            paths.going_path.latlngs.push(point);
                        }
                        
                        if (requestedDirection === "return" && direction === "return"){
                            paths.return_path.latlngs.push(point);
                        }
                    }

                    if (point.lat < min_lat)
                        min_lat = point.lat;
                    if (point.lat > max_lat)
                        max_lat = point.lat;                                
                    if (point.lng < min_lng)
                        min_lng = point.lng;
                    if (point.lng > max_lng)
                        max_lng = point.lng;

                    // https://github.com/pointhi/leaflet-color-markers
                    var iconUrl;
                    if (direction == "going")
                        iconUrl = 'https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png';
                    else
                        iconUrl = 'https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png';

                    var marker = {
                        lat: point.lat,
                        lng: point.lng,
                        message: "Bus stop: <strong>" + stop.id + "</strong>",
                        focus: false,
                        draggable: false,
                        icon:{
                            iconUrl: iconUrl,
                            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                            iconSize: [25, 41],
                            iconAnchor: [12, 41],
                            popupAnchor: [1, -34],
                            shadowSize: [41, 41]
                        },
                        id: stop.id
                    }

                    markers["busStop"+stop.id] = marker;
                });

                PathInfo.bounds = [
                                    [max_lat, max_lng],
                                    [min_lat, min_lng]
                                ];

                PathInfo.paths = paths;

                PathInfo.markers = markers;

                return PathInfo;
            },
            queryBusStop : function (id) {
                for (var i=0; i < linee.stops.length; i++ ) {
                    if (linee.stops[i].id == id)
                        return linee.stops[i].lines;
                }
            }
        };
    }
]);

app.directive('myDirective', function () {
  /*
  return {

  };
  */
});