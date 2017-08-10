app.controller('CalculateController', ['$scope', 'PathsDataProvider', 'leafletBoundsHelpers',
    function ($scope, PathsDataProvider, leafletBoundsHelpers) {
        angular.extend($scope, {
            turin: {
                        lat: 45.07,
                        lng: 7.69,
                        zoom: 13
            },
            bounds: {},
            paths: {},
            markers: {},
            defaults: {
                tileLayer: "https://api.mapbox.com/styles/v1/mapbox/streets-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1IjoiZGF2cjA5MTAiLCJhIjoiY2owemk4N2FmMDJ1ZzMzbno3YjZxZDN3YyJ9.eJdGDM0goIVXcFmMrQX8og",
            },
            legend: {
                position: 'bottomleft',
                colors: [ 'blue', 'orange'],
                labels: [ 'A piedi', 'Bus' ]
            }
        });

        $scope.pathDetailsVisibility = true;
        $scope.pathDetails = PathsDataProvider.getPathDetails();
        this.calculate = function(){

            //Use Nominatim service to get lat and long for the address
            var pathPrefix = "https://nominatim.openstreetmap.org/search?q=";

            //build source query by replacing space for +
            var src_addr = this.source.replace(/ /g, "+");

            //build source query by replacing space for +
            var dst_addr = this.destination.replace(/ /g, "+");

            PathsDataProvider.setSource(pathPrefix+src_addr+"+Torino&format=json");  //add Nominatim suffix
            PathsDataProvider.setDestination(pathPrefix+dst_addr+"+Torino&format=json");


            //var self = this;

            PathsDataProvider.getPath().then(function (PathInfo){

                var polylines = PathInfo.polylines;

                $scope.paths = {};
                for (var i = 0; i < polylines.length; i++)
                    $scope.paths['line' + i] = polylines[i];

                $scope.bounds = leafletBoundsHelpers.createBoundsFromArray(PathInfo.bounds);
                $scope.markers = PathInfo.markers;
                $scope.pathDetailsVisibility = false;

            });


        }


    }
]);

app.factory('PathsDataProvider', [ '$http', '$window',
    function ($http, $window) {

        var source,destination;

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

                var markers = {};
                var srcLat, srcLong, dstLat, dstLong;

                //Get lat and long for source address
                return $http.get(source).then(function sourceSuccessCallback(srcData) {

                    //Check if the address exists
                    if (srcData.length == 0){

                        $window.alert("Indirizzo sorgente non trovato");
                        return $http.promise.reject();

                    }
                    else {

                        //Parse response to get source lat and long

                        //Get only first element

                        srcLat = srcData.data[0].lat;
                        srcLong = srcData.data[0].lon;
                        
                        return $http.get(destination);
                    }



                }).then(function destinationSuccessCallback(dstData){

                    //Get lat and long for destination address

                    //Check if the address exists

                    if (dstData.length == 0){

                        $window.alert("Indirizzo destinazione non trovato");
                        return $http.promise.reject();
                    }
                    else {

                        //Parse response to get destination lat and long

                        dstLat = dstData.data[0].lat;
                        dstLong = dstData.data[0].lon;

                        //Request best path

                        return $http.get("/rest/path?srcLat="+srcLat+"&srcLong="+srcLong+"&dstLat="+dstLat+"&dstLong="+dstLong);

                    }
                }).then(function pathRequestSuccess(resp){


                    var bestPath = resp.data;

                    var PathInfo = new Object();

                    var max_lat,max_lng,min_lat,min_lng;
                    min_lat = 90; max_lat = -90; min_lng = 180; max_lng = -180;

                    var firstElement = true;

                    // empty the current path array for html details
                    currentPath.length = 0;

                    var curPolyline = {};
                    var previousMode;
                    var previousLine;
                    var polylines = [];
                    var myline;


                    for (var i = 0; i < bestPath.length; i++){
                        var edge = bestPath[i];

                        var pointSrc = new Object();
                        var pointDst = new Object();

                        if (firstElement){
                            previousMode = edge.mode;
                            previousLine = edge.edgeLine;
                            firstElement = false;

                            var srcMarker = {
                                lat: parseFloat(edge.latSrc),
                                lng: parseFloat(edge.lonSrc),
                                message: "Partenza",
                                focus: false,
                                draggable: false,
                                icon:{
                                    iconUrl: 'http://icons.iconarchive.com/icons/everaldo/crystal-clear/72/Action-flag-icon.png',
                                    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                                    iconSize: [48, 48],
                                    iconAnchor: [12, 41],
                                    popupAnchor: [1, -34],
                                    shadowSize: [41, 41]
                                },
                                id: 1
                            }

                            markers['src'] = srcMarker;

                            curPolyline = createPolyline(edge.mode, edge.edgeLine);
                        }
                        else{
                            if ( previousMode != edge.mode || previousLine != edge.edgeLine){
                                polylines.push(curPolyline);

                                previousMode = edge.mode;

                                curPolyline = createPolyline(edge.mode, edge.edgeLine);
                            }
                        }

                        previousLine = edge.edgeLine;

                        pointSrc.lat = edge.latSrc;
                        pointSrc.lng = edge.lonSrc;

                        pointDst.lat = edge.latDst;
                        pointDst.lng = edge.lonDst;

                        curPolyline.latlngs.push(pointSrc);
                        curPolyline.latlngs.push(pointDst);

                        //Necessary for adding the last path
                        if(i == bestPath.length-1){
                            polylines.push(curPolyline);

                            var dstMarker = {
                                lat: parseFloat(edge.latDst),
                                lng: parseFloat(edge.lonDst),
                                message: "Arrivo",
                                focus: false,
                                draggable: false,
                                icon:{
                                    iconUrl: 'http://icons.iconarchive.com/icons/icons8/ios7/72/Sports-Finish-Flag-icon.png',
                                    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                                    iconSize: [48, 48],
                                    iconAnchor: [12, 41],
                                    popupAnchor: [1, -34],
                                    shadowSize: [41, 41]
                                },
                                id: 2
                            }

                            markers['dst'] = dstMarker;

                        }

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

                            //Necessary for adding the last edge
                            if(i == bestPath.length-1) {
                                currentPath.push(myline);

                                var dstMarker = {
                                    lat: parseFloat(edge.latDst),
                                    lng: parseFloat(edge.lonDst),
                                    message: "Arrivo",
                                    focus: false,
                                    draggable: false,
                                    icon:{
                                        iconUrl: 'http://icons.iconarchive.com/icons/icons8/ios7/72/Sports-Finish-Flag-icon.png',
                                        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                                        iconSize: [48, 48],
                                        iconAnchor: [12, 41],
                                        popupAnchor: [1, -34],
                                        shadowSize: [41, 41]
                                    },
                                    id: 2
                                }

                                markers['dst'] = dstMarker;
                            }

                            continue;
                        }
                        else {
                            currentPath.push(myline);
                            myline = new Object();
                        }
                        myline.cost = edge.cost;

                        myline.edgeLine = edge.edgeLine;
                        myline.mode = edge.mode;

                        //Necessary for adding the last edge
                        if(i == bestPath.length-1)
                            currentPath.push(myline);
                    }

                    PathInfo.bounds = [
                        [max_lat, max_lng],
                        [min_lat, min_lng]
                    ];


                    PathInfo.polylines = polylines;
                    PathInfo.markers = markers;

                    return PathInfo;




                }).catch(function errorCallback(error){

                    //Handle the error by showing an alert box 'Nessun risultato'
                    $window.alert("Nessun risultato");
                });


            }
        };
    }
]);

function createPolyline(mode,line){
    polyline = {
        type: "polyline",
        weight: 6,
        color: '',
        latlngs: []
    };

    if (mode == 0){
        polyline.color = "orange";
        polyline.dashArray = "";
        polyline.message = "Line: "+line;
    }
    else{
        polyline.color = "blue";
        polyline.dashArray = "5, 10";
    }

    return polyline
}
