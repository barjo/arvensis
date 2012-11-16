'use strict';


// Declare app level module which depends on filters, and services
angular.module('myApp', ['myApp.filters', 'myApp.services']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/inspect-machines', {templateUrl: 'partials/inspect-machines.html', controller: InspectMachinesCtrl});
    $routeProvider.when('/inspect-exported', {templateUrl: 'partials/inspect-exported.html', controller: InspectExportedCtrl});
    $routeProvider.otherwise({redirectTo: '/inspect-machines'});
  }]);


