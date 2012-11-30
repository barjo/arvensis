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


function ManageMachinesCtrl($scope, $rootScope, $location ,Machine) {
  $rootScope.currentView="manage-machines";

  //Get All existing RoSe machines
  $scope.machines=Machine.query();

  //Set new machine default host
  $scope.machine = { host : $location.host()};

  $scope.createMachine = function (){
  	  var machine = new Machine($scope.machine);
	  machine.$save(function (){
		$scope.machines.push(machine);	
	});	
  };
}

function ManageMachineCtrl($scope,$rootScope, $routeParams, Machine) {
	$rootScope.currentView = "manage-machines";
	$scope.machine = Machine.get({id : $routeParams.machineId});
}

