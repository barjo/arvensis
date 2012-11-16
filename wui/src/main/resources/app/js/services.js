'use strict';

/* Services */


angular.module('myApp.services', ['ngResource']).
    factory('Inspect', function($resource){
  		return $resource('../inspect/:type', {}, {
   			exported : {method:'GET', params:{type:'exported'}, isArray:true},
   			imported : {method:'GET', params:{type:'imported'}, isArray:true},
			disco : {method:'GET', params:{type:'discovered'}, isArray:true},
   			machines : {method:'GET', params:{type:'machines'}, isArray:true}
  		});
	});
