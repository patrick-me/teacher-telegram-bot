/**
 * Created by Patrick on 10.02.2018.
 */

angular.module('bot-app', ['angular.filter'])
    .controller('bot-controller', function ($scope, $http) {

        $scope.getLessons = function () {
            $http.get("/lessons")
                .then(function (response) {
                    $scope.lessons = response.data;
                });
        };

        $scope.getLessons();

        $scope.save = function (lesson) {
            //ToDo: validation
            $http.post("/lessons", lesson)
                .then(function (response) {
                    $scope.getLessons();
                });
        };
    });