'use strict';


// Declare app level module which depends on filters, and services
angular.module('myApp', ['myApp.filters', 'myApp.services']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/inspect-machines', {templateUrl: 'partials/inspect-machines.html', controller: InspectMachinesCtrl});
    $routeProvider.when('/inspect-endpoints', {templateUrl: 'partials/inspect-endpoints.html', controller: InspectEndpointsCtrl});
    $routeProvider.otherwise({redirectTo: '/inspect-machines'});
  }]);


