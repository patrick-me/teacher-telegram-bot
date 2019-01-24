/**
 * Created by Patrick on 10.02.2018.
 */

angular.module('bot-app', ['angular.filter', 'ngRoute'])
    .directive("fileread", [function () {
        return {
            scope: {
                fileread: "="
            },
            link: function (scope, element, attributes) {
                element.bind("change", function (changeEvent) {
                    var reader = new FileReader();
                    reader.onload = function (loadEvent) {
                        scope.$apply(function () {
                            scope.fileread = loadEvent.target.result;
                        });
                    };
                    reader.readAsDataURL(changeEvent.target.files[0]);
                });
            }
        }
    }])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider
            .when("/lessons", {templateUrl: 'templates/lessons/lessons.html'})
            .when("/questionTypes", {templateUrl: 'templates/questionTypes.html'})
            .when("/sentences", {templateUrl: 'templates/sentences/sentences.html'})
            .when("/users", {templateUrl: 'templates/users.html'})
            .when("/bots", {templateUrl: 'templates/bots.html'})
            .when("/pandas", {templateUrl: 'templates/pandas/pandas.html'})
            .otherwise({templateUrl: 'templates/lessons/lessons.html'});
    }])
    .controller('bot-controller', function ($scope, $http) {

        $(function () {
            $('[data-toggle="tooltip"]').tooltip()
        });

        $scope.pandaAlert = "";
        $scope.sentenceAlert = "";
        $scope.lessonAlert = "";

        $scope.currentPandaTemplate = '';
        $scope.currentSentenceTemplate = '';
        $scope.currentLessonTemplate = '';


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

        $scope.getPandas = function () {
            $http.get("/pandas")
                .then(function (response) {
                    $scope.pandas = response.data;
                });
        };

        $scope.getConfig = function () {
            $http.get("/configs/numberHowOftenSendPandas")
                .then(function (response) {
                    $scope.pandaSendNumber = response.data;
                });
        };

        $scope.getSentenceQTforTooltip = function (sentence) {
            if (sentence.questions.length > 0) {
                var text = "";
                angular.forEach(sentence.questions, function (v) {
                    text += v.questionType.name + "\n";
                });
                return text.substring(0, text.length - 1);
            }
            return "There are no questions";
        };

        $scope.getBots = function () {
            $http.get("/bots")
                .then(function (response) {
                    $scope.bots = response.data;
                });
        };

        $scope.getUsers = function () {
            $http.get("/users")
                .then(function (response) {
                    $scope.users = response.data;
                });
        };

        $scope.getLessons();
        $scope.getQuestionTypes();
        $scope.getSentences();
        $scope.getBots();
        $scope.getUsers();
        $scope.getPandas();
        $scope.getConfig();

        $scope.saveLesson = function (lesson) {
            //ToDo: validation
            var alqt = angular.copy(lesson.questionTypes);

            if (alqt) {
                for (var i = alqt.length - 1; i >= 0; i--) {
                    if (!alqt[i].active) {
                        alqt.splice(i, 1);
                    }
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
            $scope.sentenceAlert = "";

            $http.post("/sentences", sentence)
                .then(function (response) {
                    $scope.getSentences();
                });
        };

        $scope.savePanda = function (panda) {
            $scope.pandaAlert = "";
            $http.post("/pandas", panda)
                .then(function (response) {
                    $scope.getPandas();
                });
        };

        $scope.saveConfig = function (config) {
            $http.post("/configs", config)
                .then(function (response) {
                    $scope.getConfig();
                });
        };

        $scope.deleteSentence = function (entity) {
            $scope.sentenceAlert = "Предложение '" + entity.name + "' удалено со всеми зависимостями!";

            $http.delete("/sentences/" + entity.id)
                .then(function (response) {
                    $scope.getSentences();
                });
        };

        $scope.deleteLesson = function (entity) {
            $scope.lessonAlert = "Урок '" + entity.name + "' удален. Предложения не удаляются!";

            $http.delete("/lessons/" + entity.id)
                .then(function (response) {
                    $scope.getLessons();
                });
        };

        $scope.checkIfExist = function (entity) {
            var existed = false;
            angular.forEach($scope.sentences, function (value) {
                if (value.name == entity.name) {
                    existed = true;
                    $scope.sentenceAlert = 'Предложение с таким названием уже существует!';
                }
            });

            return existed;
        };

        $scope.saveBot = function (bot) {
            $http.post("/bots", bot)
                .then(function (response) {
                    $scope.getBots();
                });
        };

        $scope.getLessonQuestionTypes = function (lesson) {
            if (lesson === undefined || lesson === null) {
                return angular.copy($scope.questionTypes);
            }

            var hash = {};
            angular.forEach(lesson.questionTypes, function (value) {
                if (value.active === undefined) {
                    value.active = true;
                }
                hash[value.id] = value;
            });

            //alert(JSON.stringify(hash, null, 4));
            var qt = angular.copy($scope.questionTypes);
            angular.forEach(qt, function (value) {
                var h = hash[value.id];
                value.active = h !== undefined && h.active;
            });
            return qt;
        };

        $scope.getUserLessons = function (user) {
            if (user === undefined || user === null) {
                console.log("User is undefined");
                return [];
            }

            $scope.getLessons();
            $scope.getUserLessonsById(user.id);
        };

        $scope.getUserLessonsById = function (id) {
            $scope.userLessons = undefined;

            $http.get("/lessons/user/" + id)
                .then(function (response) {
                    $scope.userLessons = response.data;

                    var lessons = angular.copy($scope.lessons);
                    var userLessons = angular.copy($scope.userLessons);

                    var hash = {};
                    angular.forEach(userLessons, function (value) {
                        hash[value.id] = value;
                    });

                    angular.forEach(lessons, function (value) {
                        value.active = hash[value.id] !== undefined;
                    });
                    $scope.allUserLessons = lessons;
                });
        };

        $scope.currentUserChanged = function (user) {
            $scope.currentUser = user;
            $scope.getUserLessons($scope.currentUser);
        };

        $scope.saveUserLessons = function (userId, userLessons) {
            var ul = angular.copy(userLessons);
            for (var i = ul.length - 1; i >= 0; i--) {
                if (!ul[i].active) {
                    ul.splice(i, 1);
                }
            }

            $http.post("/lessons/user/" + userId, ul)
                .then(function (response) {
                    $scope.getUsers();
                });
        };

        $scope.addQuestion = function (sentence, questionType) {
            if (!sentence) {
                $scope.sentenceAlert = "Для того чтобы добавить вопрос, необходимо добавить предложение";
                return false;
            }

            var question = {
                "questionType": angular.copy(questionType),
                "highlightedSentence": "",
                "question": "",
                "keyboard": ""
            };

            if (!sentence.questions) {
                sentence.questions = [];
            }
            sentence.questions.push(question);
        };

        $scope.removeQuestion = function (sentence, questionType) {
            var idx = sentence.questions.indexOf(questionType);
            sentence.questions.splice(idx, 1);
        };

        $scope.shuffleKeyBoard = function (keyboard) {
            var array = keyboard.split(' ; ');
            array = shuffle(array);
            keyboard = array.join(' ; ');
            return keyboard;
        };

        function shuffle(array) {
            var m = array.length, t, i;

            // While there remain elements to shuffle…
            while (m) {

                // Pick a remaining element…
                i = Math.floor(Math.random() * m--);

                // And swap it with the current element.
                t = array[m];
                array[m] = array[i];
                array[i] = t;
            }

            return array;
        }
    });