app.controller('MainCtrl', [ '$scope', 'LinesDataProvider', 'leafletBoundsHelpers', '$routeParams', '$location', 'linesCache',
    function ($scope, LinesDataProvider, leafletBoundsHelpers, $routeParams, $location, linesCache) {

        document.getElementById("direction-buttons").style.visibility = "hidden";

        this.lineID = $routeParams.lineID;

        var self = this;


        //Use cache to store the lines when they are downloaded. This to avoid to download
        // them again when the view changes

        var cache = linesCache;


        if(angular.isUndefined(cache.get('lines'))){

            LinesDataProvider.lines_request().then(
                function(res){
                    self.lines = res;
                    cache.put('lines',res);
                });
        }else
            this.lines = cache.get('lines');



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

            var PathInfo = LinesDataProvider.loadPath($routeParams.lineID,'all');
            $scope.paths = PathInfo.paths;
            $scope.markers = PathInfo.markers;
            $scope.bounds = leafletBoundsHelpers.createBoundsFromArray(PathInfo.bounds);
            document.getElementById("direction-buttons").style.visibility = "visible";
        } else {
            $scope.center = { lat: 45.07,
                         lng: 7.69,
                         zoom: 13
                     }
        };

        $scope.$on('leafletDirectiveMarker.click', function(e, args) {
            clickedBusStopId = args.model.id;

                LinesDataProvider.queryBusStop.query({stop: clickedBusStopId},function(busStopLines){

                var busStopLinesHTML = "<br>Altre linee:";
                angular.forEach(busStopLines, function(value, key){
                    busStopLinesHTML += "<br><img src='../assets/bus_popup.png' width='20px'>"+"&nbsp;&nbsp;"+value+"</img>";
                });

                var popup = args.leafletObject._popup;

                popup.setContent("<span style='color: #2795e7;'><strong>"+"FERMATA  "+"</strong></span><strong>" + clickedBusStopId + "</strong>" + busStopLinesHTML);

            });


        });


        $scope.buttonClick = function(direction){

            /*//Clear map
            $scope.paths = {};
            $scope.markers = {};*/

            var PathInfo = LinesDataProvider.loadPath($routeParams.lineID,direction);
            $scope.paths = PathInfo.paths;
            $scope.markers = PathInfo.markers;
            $scope.bounds = leafletBoundsHelpers.createBoundsFromArray(PathInfo.bounds);

        }


    }
]);

app.factory('LinesDataProvider', ['$resource', '$filter',
    function ($resource,$filter) {

        var lines_resource = $resource('/rest/lines/');
        var lines = {};

        var stops = {};
        var stops_resource = $resource('/rest/stops/').query(function(data){
            stops = data;
        });

        var stop_lines = $resource('/rest/stops/:stop');

        return {
            lines_request : function(){
                var data = lines_resource.query().$promise.then(function(d){
                    lines = d;
                    return lines;
                }).catch(function(error){
                    //lines = error;
                    bootstrap_alert_warning("Ci sono problemi. Riprovare più tardi");
                });
                return data;
            },

            query : function (id) {
                for (var i=0; i < lines.length; i++ ) {
                    if (lines[i].line == id)
                        return lines[i].busStops;
                }
            },
            loadPath : function (id,reqDir) {
                var PathInfo = new Object();

                var direction = "going";
                var requestedDirection = reqDir;
                var goingPhase = true;
                var firstElement = true;
                var firstBusId;
                var lastProcessedBusId;
                var end = false;


                var max_lat,max_lng,min_lat,min_lng;
                min_lat = 90; max_lat = -90; min_lng = 180; max_lng = -180;

                var paths = {
                    going_path : {
                        type : "polyline",
                        weight: 4,
                        color: 'green',
                        message: "Linea: <strong>" + id + "</strong>",
                        latlngs : []
                    },
                    return_path : {
                        type : "polyline",
                        weight: 4,
                        color: 'blue',
                        message: "Linea: <strong>" + id + "</strong>",
                        latlngs : []
                    }
                };

                var markers = {};

                var stops_res = this.query(id);

                for(var i = 0; i<stops_res.length; i++){

                    var stop = stops_res[i];
                    var point = new Object();

                    var stop = $filter('filter')(stops, function (s) {
                        return s.id === stop.busStop.id;})[0];

                    if ( firstElement ){
                        firstBusId = stop.id;
                        lastProcessedBusId = firstBusId;
                        firstElement = false;
                    }
                    else{
                        if (stop.id === lastProcessedBusId){
                            direction = "return";
                            end = true;
                            if(end && requestedDirection === 'going')
                                break;

                            if(requestedDirection === "return")
                                markers = {}; //empty markers in order to consider going stop
                        }

                        else
                            lastProcessedBusId = stop.id;
                    }

                    point.lat = stop.lat;
                    point.lng = stop.lng;

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
                        message: "<span style='color: #2795e7;'><strong>"+"FERMATA"+"</strong></span><strong>" + stop.id + "</strong>",
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
                }



                PathInfo.bounds = [
                                    [max_lat, max_lng],
                                    [min_lat, min_lng]
                                ];

                PathInfo.paths = paths;

                PathInfo.markers = markers;

                return PathInfo;
            },
            queryBusStop : stop_lines
        };
    }
]);

app.directive('myDirective', function () {
  /*
  return {

  };
  */
});

/*
app.filter('linesFilter', function () {
	  return function (items,searchLine) {
	    var filtered = [];
	    
	    for (var i = 0; i < items.length; i++) {
	      var item = items[i];
	      if (searchLine === item.line) {
	        filtered.push(item);
	      }
	    }
	    return filtered;
	  };
});*/