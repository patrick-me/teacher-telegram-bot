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
        $scope.alphaLengthComparator = function (v1, v2) {
            // If we don't get strings, just compare by index
            if (v1.type !== 'string' || v2.type !== 'string') {
                return (v1.index < v2.index) ? -1 : 1;
            }

            if (v1.value.length !== v2.value.length) {
                return (v1.value.length < v2.value.length) ? -1 : 1;
            }

            // Compare strings alphabetically, taking locale into account
            return v1.value.localeCompare(v2.value);
        };

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

        $scope.saveSentence = function (sentence) {
            $http.post("/sentences", sentence)
                .then(function (response) {
                    $scope.getSentences();
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

        $scope.addQuestion = function (sentence, questionType) {
            var question = {
                "questionType": angular.copy(questionType),
                "highlightedSentence": "",
                "question": "",
                "keyboard": ""
            };

            if(!sentence.questions) {
                sentence.questions = [];
            }
            sentence.questions.push(question);
        };

        $scope.removeQuestion = function(sentence, questionType) {
            var idx = sentence.questions.indexOf(questionType);
            sentence.questions.splice(idx, 1);
        };
    });