app.controller('MainCtrl', [ '$scope', 'LinesDataProvider', 'leafletBoundsHelpers', '$routeParams', '$location', 'linesCache',
    function ($scope, LinesDataProvider, leafletBoundsHelpers, $routeParams, $location, linesCache) {

        document.getElementById("direction-buttons").style.visibility = "hidden";

        this.lineID = $routeParams.lineID;

        var lines_vect = [];

        var self = this;

        var page = 0;
        var per_page = 5;

        this.pull_lines = function(res,page,per_page) {

            if(res !== undefined){

                if (res.length === 0){

                    cache.put('lines',lines_vect);
                    return;

                }
                else {
                    lines_vect.push.apply(lines_vect, res);
                    self.lines = lines_vect;

                    LinesDataProvider.lines_request(page+1,per_page).then(function(res){
                        self.pull_lines(res,page+1,per_page)
                    });
                }
            }

        };


        //Use cache to store the lines when they are downloaded. This to avoid to download
        // them again when the view changes

        var cache = linesCache;

        if(angular.isUndefined(cache.get('lines'))){



            LinesDataProvider.lines_request(page,per_page).then(
                function(res){

                    self.pull_lines(res,page,per_page);
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
            var clickedBusStopId = args.model.id;

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
            var PathInfo = LinesDataProvider.loadPath($routeParams.lineID,direction);
            $scope.paths = PathInfo.paths;
            $scope.markers = PathInfo.markers;
            $scope.bounds = leafletBoundsHelpers.createBoundsFromArray(PathInfo.bounds);

        }


    }
]);