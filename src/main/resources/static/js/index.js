/**
 * Created by Patrick on 10.02.2018.
 */

angular.module('bot-app', ['angular.filter', 'ngRoute'])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider
            .when("/lessons", {templateUrl: 'templates/lessons.html'})
            .when("/questionTypes", {templateUrl: 'templates/questionTypes.html'})
            .when("/sentences", {templateUrl: 'templates/sentences.html'})
            .when("/users", {template: '<h4>Users</h4>'})
            .when("/bots", {template: '<h4>Bots</h4>'})
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
                    angular.forEach($scope.questionTypes, function (value) {
                        value.active = false;
                    });
                });
        };

        $scope.getSentences = function () {
            $http.get("/sentences")
                .then(function (response) {
                    $scope.sentences = response.data;
                });
        };

        $scope.getLessons();
        $scope.getQuestionTypes();
        $scope.getSentences();

        $scope.save = function (lesson) {
            //ToDo: validation
            var alqt = angular.copy($scope.allLessonQuestionTypes);
            for (var i = alqt.length - 1; i >= 0; i--) {
                if (!alqt[i].active) {
                    alqt.splice(i, 1);
                }
            }

            lesson.questionTypes = alqt;
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

        $scope.currentLesson = {};
        $scope.allLessonQuestionTypes = angular.copy($scope.questionTypes);

        $scope.getLessonQuestionTypes = function (lesson) {
            if (lesson === undefined || lesson === null) {
                return $scope.questionTypes;
            }

            var hash = {};
            angular.forEach(lesson.questionTypes, function (value) {
                hash[value.id] = value;
            });
            var qt = angular.copy($scope.questionTypes);
            angular.forEach(qt, function (value) {
                value.active = hash[value.id] !== undefined;
            });
            return qt;
        };


        $scope.currentLessonChanged = function (cl) {
            $scope.currentLesson = cl;
            $scope.allLessonQuestionTypes = $scope.getLessonQuestionTypes($scope.currentLesson);
        };
    });