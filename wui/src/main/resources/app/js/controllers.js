'use strict';

/* Controllers */


function InspectMachinesCtrl($scope, $rootScope, Inspect) {
  $rootScope.currentView="inspect-machines";
  
  $scope.machines = Inspect.machines();
  $scope.orderProp = 'date';

  $scope.getDate = function(machine){
  	return new Date(machine.date);
  };
}

function InspectEndpointsCtrl($scope,$rootScope, Inspect) {
  $scope.getExported = function(){
  	return Inspect.exported();
  }
  
  $scope.getImported = function(){
  	return Inspect.imported();
  }
  
  $scope.getDisco = function(){
  	return Inspect.disco();
  }
  
  $rootScope.currentView="inspect-endpoints"; 
  $scope.endpointType = "exported";
  $scope.endpoints = $scope.getExported();
  $scope.orderProp = 'endpoint.framework.uuid';
}

