app.controller('CalculateController', ['$scope', 'PathsDataProvider', 'leafletBoundsHelpers',
    function ($scope, PathsDataProvider, leafletBoundsHelpers,leafletData) {

        var self = $scope;
        initialize_map($scope);

        $scope.pathDetailsVisibility = true;
        $scope.pathDetails = PathsDataProvider.getPathDetails();
        this.calculate = function(){


            //Refresh the map otherwise precedent informations will be present (e.g. popup line)
            initialize_map(self);

            //Use Nominatim service to get lat and long for the address
            //var pathPrefix = "https://nominatim.openstreetmap.org/search?q=";

            //Use ArcGis service to get lat ando long for the address
            var pathPrefix = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates?SingleLine=";


            //build source query by replacing space for +
            var src_addr = $scope.source.replace(/ /g, "+");


            //build destination query by replacing space for +
            var dst_addr = $scope.destination.replace(/ /g, "+");

            PathsDataProvider.setSource(pathPrefix+src_addr+"&category=&outFields=*&forStorage=false&f=pjson");  //add ArcGis suffix
            PathsDataProvider.setDestination(pathPrefix+dst_addr+"&category=&outFields=*&forStorage=false&f=pjson");


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

        //Use to show suggestions while user typing a character, contacting the arcgis service
        $scope.getSrcSuggestions = function(){
             PathsDataProvider.getSuggestions($scope.source.replace(/ /g,"%2C")).then(function(sugs){

                 $scope.srcSuggestions = sugs;
             })
        }

        //Use to show suggestions while user typing a character, contacting the arcgis service
        $scope.getDstSuggestions = function(){
            PathsDataProvider.getSuggestions($scope.destination.replace(/ /g,"%2C")).then(function(sugs){

                $scope.dstSuggestions = sugs;
            })
        }

    }
]);

app.factory('PathsDataProvider', [ '$http', '$window',
    function ($http, $window) {

        var source,destination;

        var currentPath = [];

        //Used for get suggestions while user typing the address

        function getSuggestions(srcAddress){

            return $http.get("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/suggest?text="+srcAddress+"&f=pjson&searchExtent=7.465761,44.948028,7.875002,45.163394&sourceCountry=ITA").then(
                function(resp){

                    var data = resp.data.suggestions;
                    var suggestions = [];
                    for(var i = 0; i < data.length; i++){
                        suggestions.push(data[i].text);
                    }
                    return suggestions;
                }).catch(function(){

                    //There are problems: no response or error in response
                    bootstrap_alert_warning("Nessuna risposta dal server. Provare più tardi");
                    window.setTimeout(function() {
                            $(".alert_placeholder").fadeTo(500, 0).slideUp(500, function(){
                            $(this).remove();
                        });
                    }, 5000);
            });

        }


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

                        //$window.alert("Indirizzo sorgente non trovato");
                        return $http.promise.reject();

                    }
                    else {

                        //Parse response to get source lat and long

                        //Get only first element

                        srcLat = srcData.data.candidates[0].location.y;
                        srcLong = srcData.data.candidates[0].location.x;
                        
                        return $http.get(destination);
                    }



                }).then(function destinationSuccessCallback(dstData){

                    //Get lat and long for destination address

                    //Check if the address exists

                    if (dstData.length == 0){

                        //$window.alert("Indirizzo destinazione non trovato");
                        return $http.promise.reject();
                    }
                    else {

                        //Parse response to get destination lat and long

                        dstLat = dstData.data.candidates[0].location.y;
                        dstLong = dstData.data.candidates[0].location.x;

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
                                    iconUrl: '../assets/flag-128.png',
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
                                    iconUrl: '../assets/finish-flag-128.png',
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
                                        iconUrl: '../assets/finish-flag-128.png',
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
                    //$window.alert("Nessun risultato");
                    //showAlertTypeSelector();
                    bootstrap_alert_warning("Nessun risultato trovato!");
                    window.setTimeout(function() {
                        $(".alert").fadeTo(500, 0).slideUp(500, function(){
                            $(this).remove();
                        });
                    }, 5000);
                });


            },

            getSuggestions: getSuggestions


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
        polyline.message = "Linea: "+"<strong>"+line+"</strong>";
    }
    else{
        polyline.color = "blue";
        polyline.dashArray = "5, 10";
    }

    return polyline
}

/* If you want to use modal instead alert
function showAlertTypeSelector() {
    $('#warning').modal({
        backdrop: 'static',
        keyboard: false
    });
}*/


//Function for show alert in case of error
function bootstrap_alert_warning(message){
    $('#alert_placeholder').html('<div class="alert alert-warning"><a class="close" data-dismiss="alert">×</a><span>'+message+'</span></div>');
}

function initialize_map(self){

    angular.extend(self, {
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

}