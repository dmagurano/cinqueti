app.controller('CalculateController', ['$scope', 'PathsDataProvider', 'leafletBoundsHelpers',
    function ($scope, PathsDataProvider, leafletBoundsHelpers) {

        var self = $scope;
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

        $scope.pathDetailsVisibility = true; //Show the box for path details
        $scope.pathDetails = PathsDataProvider.getPathDetails();  //Show the path details
        $scope.source = '';  //scope variable that will fill with the source address by the user
        $scope.destination = '';   //scope variable that will fill with the destination address by the user
        
        //Calculate and show the best path from source to destination
        this.calculate = function(){


            //Refresh the map otherwise precedent informations will be present (e.g. popup line)
            angular.extend(self, {
                bounds: {},
                paths: {},
                markers: {}
            });

            //If you want to use Nominatim service to get lat and long for the address
            //var pathPrefix = "https://nominatim.openstreetmap.org/search?q=";

            //Use ArcGis service to get latitude and longitude for the address
            var pathPrefix = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates?SingleLine=";


            //build source query by replacing space for +
            var src_addr = $scope.source.replace(/ /g, "+");


            //build destination query by replacing space for +
            var dst_addr = $scope.destination.replace(/ /g, "+");

            //The searchExtent parameter limits the search around the Torino area (e.g. Torino,Grugliasco, Rivoli ecc.)
            var postfix = "&category=&outFields=*&forStorage=false&f=pjson&searchExtent=7.465761,44.948028,7.875002,45.163394&sourceCountry=ITA";

            PathsDataProvider.setSource(pathPrefix+src_addr+postfix);  //set address source 
            PathsDataProvider.setDestination(pathPrefix+dst_addr+postfix);	//set destination address

            PathsDataProvider.getPath().then(function (PathInfo){

                var polylines = PathInfo.polylines;  //The path is a set of polyline objects

                $scope.paths = {};
                for (var i = 0; i < polylines.length; i++)
                    $scope.paths['line' + i] = polylines[i];

                $scope.bounds = leafletBoundsHelpers.createBoundsFromArray(PathInfo.bounds);
                $scope.markers = PathInfo.markers;
                $scope.pathDetailsVisibility = false;

            });


        }

        //Use to show suggestions while typing a character, contacting the arcgis service
        $scope.getSrcSuggestions = function(){
            if($scope.source === undefined || $scope.source === '')
                return;
            PathsDataProvider.getSuggestions($scope.source.replace(/ /g,"%2C")).then(function(sugs){

                $scope.srcSuggestions = sugs;  //Fill the suggestions box with the suggestions
                							   //for source address
            })
        }

        //Use to show suggestions while user typing a character, contacting the arcgis service
        $scope.getDstSuggestions = function(){
            if($scope.destination === undefined || $scope.destination === '')
                return;
            PathsDataProvider.getSuggestions($scope.destination.replace(/ /g,"%2C")).then(function(sugs){

                $scope.dstSuggestions = sugs;	//Fill the suggestions box with the suggestions
				   								//for destination address
            })
        }

    }
]);
