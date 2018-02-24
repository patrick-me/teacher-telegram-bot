/**
 * Created by Patrick on 10.02.2018.
 */

angular.module('bot-app', ['angular.filter', 'ngRoute'])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider
            .when("/lessons", {
                templateUrl: 'templates/lessons.html'
            })
            .when("/questionTypes", {
                templateUrl: 'templates/questionTypes.html'
            })
            .when("/users", {
                template: '<p>User\'s content</p>'
            })
            .when("/bots", {
                template: '<p>Bot\'s content</p>'
            })
            .otherwise({
                template: 'This is main'
            });
    }])
    .controller('bot-controller', function ($scope, $http) {

        $scope.getLessons = function () {
            $http.get("/lessons")
                .then(function (response) {
                    $scope.lessons = response.data;
                });
        };

        $scope.getQuestionTypes = function () {
            $http.get("/questionTypes")
                .then(function (response) {
                    $scope.questionTypes = response.data;
                });
        };

        $scope.getLessons();
        $scope.getQuestionTypes();

        $scope.save = function (lesson) {
            //ToDo: validation
            $http.post("/lessons", lesson)
                .then(function (response) {
                    $scope.getLessons();
                });
        };

        $scope.saveQT = function (questionType) {
            //ToDo: validation
            $http.post("/questionTypes", questionType)
                .then(function (response) {
                    $scope.getQuestionTypes();
                });
        };

        $scope.addQTtoLesson = function(lesson, qt){
            var index = lesson.questionTypes.indexOf(qt);
            if (index >= 0) {
                lesson.questionTypes.splice(index, 1);
            } else {
                lesson.questionTypes.push(qt);
            }
            console.log(lesson.questionTypes);
        };
    });