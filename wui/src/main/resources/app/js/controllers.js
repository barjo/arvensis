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

function InspectExportedCtrl($scope,$rootScope, Inspect) {
  $rootScope.currentView="inspect-exported";
  
  $scope.xendpoints = Inspect.exported();
  $scope.orderProp = 'endpoint.framework.uuid';
}


function InspectImportedCtrl($scope,$rootScope, Inspect) {
  $rootScope.currentView="inspect-imported";

  $scope.iendpoints = Inspect.imported();
  $scope.orderProp = 'endpoint.framework.uuid';
}


function InspectDiscoveredCtrl($scope,$rootScope, Inspect) {
  $rootScope.currentView="inspect-discovered";

  $scope.dendpoints = Inspect.disco();
  $scope.orderProp = 'endpoint.framework.uuid';
}

