app.factory('PathsDataProvider', [ '$http',
    function ($http) {

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
                    if (srcData.data.candidates.length == 0){

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

                    if (dstData.data.candidates.length == 0){
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

                                var line_marker = {
                                    lat: parseFloat(edge.latSrc),
                                    lng: parseFloat(edge.lonSrc),
                                    focus: false,
                                    draggable: false,
                                    icon:{
                                        iconUrl: '../assets/circle-128.png',
                                        iconSize: [16, 16],
                                        shadowSize: [0, 0]
                                    }
                                }

                                markers[edge.edgeLine] = line_marker;


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