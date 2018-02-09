/**
 * Created by Patrick on 10.02.2018.
 */

angular.module('bot-app', [])
    .controller('bot-controller', function ($scope) {
        $scope.lessons = [
            {name: 'Lesson 1', group: 'Group 1'},
            {name: 'Lesson 2', group: 'Group 1'},
            {name: 'Lesson 3', group: 'Group 1'},
            {name: 'Lesson 4', group: 'Group 1'},
            {name: 'Lesson 5', group: 'Group 1'},
            {name: 'Lesson 6', group: 'Group 1'},
            {name: 'Lesson 7', group: 'Group 1'},
            {name: 'Lesson 1', group: 'Group 2'},
            {name: 'Lesson 2', group: 'Group 2'},
            {name: 'Lesson 3', group: 'Group 2'},
            {name: 'Lesson 4', group: 'Group 2'},
            {name: 'Lesson 5', group: 'Group 2'}
        ]
    });