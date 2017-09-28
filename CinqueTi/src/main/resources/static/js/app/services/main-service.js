app.factory('LinesDataProvider', ['$resource', '$filter',
    function ($resource,$filter) {

        var lines_resource = $resource('/rest/lines/');
        var lines = [];

        var stops = {};
        var stops_resource = $resource('/rest/stops/').query(function(data){
            stops = data;
        });

        var stop_lines = $resource('/rest/stops/:stop');

        return {
            lines_request : function(page, per_page){


                var data = lines_resource.query({page: page, per_page: per_page}).$promise.then(function(d){
                    //lines = d;
                    lines.push.apply(lines, d);
                    return d;
                }).catch(function(error){
                    //lines = error;
                    bootstrap_alert_warning("Ci sono problemi. Riprovare più tardi");
                    window.setTimeout(function() {
                        $(".alert").fadeTo(500, 0).slideUp(500, function(){
                            $(this).remove();
                        });
                    }, 5000);
                });
                return data;
            },

            //return all bus stops for line with the passed id
            query : function (id) {
                for (var i=0; i < lines.length; i++ ) {
                    if (lines[i].line == id)
                        return lines[i].busStops;
                }
            },
            loadPath : function (id,reqDir) {
                var PathInfo = new Object();

                var direction = "going";
                var requestedDirection = reqDir; //Indicate the path that should be shown:
                      							//-all: both going and return path
                								//-going: only going path
                								//-return: only return path
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

                //Build the path showing markers for all line bus stops
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

                    //In this case show both going and return path
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

               
                    var iconUrl;
                    if (direction == "going")
                        iconUrl = '../assets/marker-icon-green.png';
                    else
                        iconUrl = '../assets/marker-icon-blue.png';

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
