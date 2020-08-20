/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
angular.module('planTree',['step','artefacts','reportTable','dynamicForms','export'])

.directive('planTree', function(artefactTypes, $http,$timeout,$interval,stateStorage,$filter,$location, Dialogs) {
  return {
    restrict: 'E',
    scope: {
      plan: '=',
      stOnChange: '&',
      handle: '=',
      interactiveSessionHandle: '=',
      readonly: '=',
    },
    controller: function($scope,$location,$rootScope, AuthService) {
      $scope.undoStack=[];
      $scope.redoStack=[];
      
      $scope.$watch('plan',function() {
        if($scope.plan) {
          if ($scope.undoStack.length == 0) {
            var backupPlan = JSON.parse(JSON.stringify($scope.plan));
            $scope.undoStack.push(backupPlan);
            load(function(root) {
              tree.open_all();
              setupInitialState(root);
              overrideJSTreeKeyFunctions();
            });
          } else {
            load(function(root) {});
          }

        }
      });
      
      function setupInitialState(root) {
        var initialState = $rootScope.planEditorInitialState;
        if(initialState) {
          if(initialState.selectedNode) {
            tree.deselect_all(true);
            tree.select_node(initialState.selectedNode);            
          }
          if(initialState.interactive) {
            $scope.interactiveSessionHandle.start();
          }
          delete $rootScope.planEditorInitialState;
        } else {
          tree.select_node(root.id);
        }
      }
      
      function setSelectedNode(o) {
        if(o && o.length) { 
          var artefact = o[0];
          tree.deselect_all(true);
          tree._open_to(artefact.id);
          tree.select_node(artefact.id);          
          focusOnNode(artefact.id);
        }
      }
      
      function overrideJSTreeKeyFunctions() {
        kb = tree.settings.core.keyboard;
        if (kb.hasOwnProperty('up')) {
          orig = kb['up'];
          newfunction = function (e) {
            e.preventDefault();
            var o = tree.get_prev_dom(e.currentTarget);
            setSelectedNode(o);
          }
          kb['up']=newfunction;
        }
        if (kb.hasOwnProperty('down')) {
          orig = kb['up'];
          newfunction = function (e) {
            e.preventDefault();
            var o = this.get_next_dom(e.currentTarget);
            setSelectedNode(o);
          }
          kb['down']=newfunction;
        }
      }
      
      $scope.authService = AuthService;
      
      var tree;
      $('#jstree_demo_div').jstree(
				  {
					'core' : {
					  'data' : [],
					  'check_callback' : function (operation, node, node_parent, node_position, more) {
					    if(AuthService.hasRight('plan-write') && !$scope.readonly) {
					      if(operation=='move_node') {
					        return node_parent.parent?true:false;
					      } else {
					        return true;	              
					      }					      
					    } else {
					      return false;
					    }
					  }
					}, 
					"plugins" : ["dnd","contextmenu"],
					"contextmenu": {
					  "items": function ($node) {
					  //  var tree = $("#jstree_demo_div").jstree(true);
					    return {
					      "Rename": {
					        "separator_before": false,
					        "separator_after": true,
					        "label": "Rename \u00A0\u00A0(F2)",
					        "icon"       : false,
					        "action": function (obj) {
					          $scope.rename();
					        }
					        ,"_disabled" : $scope.readonly
					      },
					      "Move": {
					        "separator_before": false,
					        "separator_after": true,
					        "label": "Move",
					        "action": false,
					        "_disabled" : $scope.readonly,
					        "submenu": {
					          "Up": {
					            "seperator_before": false,
					            "seperator_after": false,
					            "label": "Up \u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0(Ctrl+Up)",
					            "icon"       : false,
					            action: function (obj) {
					              $scope.move(-1);
					            }
					            ,"_disabled" : $scope.readonly
					          },
					          "Down": {
					            "seperator_before": false,
					            "seperator_after": false,
					            "label": "Down (Ctrl+Down)",
					            "icon"       : false,
					            action: function (obj) {
					              $scope.move(1);
					            }
					            ,"_disabled" : $scope.readonly
					          }
					        }
					      },
					      "Copy": {
                  "separator_before": false,
                  "separator_after": false,
                  "label": "Copy \u00A0\u00A0(Ctrl+c)",
                  "icon"        : false,
                  "action": function (obj) {
                    $scope.copy();
                  }
					        ,"_disabled" : $scope.readonly
                },
                "Paste": {
                  "separator_before": false,
                  "separator_after": false,
                  "label": "Paste \u00A0(Ctrl+v)",
                  "icon"        : false,
                  "action": function (obj) {
                    $scope.paste();
                  }
                  ,"_disabled" : $scope.readonly
                },
					      "Delete": {
					        "separator_before": false,
					        "separator_after": true,
					        "label": "Delete (del)",
					        "icon"       : false,
					        "action": function (obj) {
					          $scope.remove();
					        }
                  ,"_disabled" : $scope.readonly
					      },
					      "Open": {
                  "separator_before": true,
                  "separator_after": false,
                  "label": "Open \u00A0\u00A0(Ctrl+o)",
                  "icon"        : false,
                  "action": function (obj) {
                    if ($node.original.planId || $node.original.callFunctionId) {
                      $scope.openSelectedArtefact();
                    }
                  }
                  ,"_disabled" : !($node.original.planId || $node.original.callFunctionId)
                },
                "SwitchDisable": {
                  "separator_before": true,
                  "separator_after": false,
                  "label": ($scope.isNodeDisabled()) ? "Enable \u00A0(Ctrl+e)" : "Disable \u00A0(Ctrl+e)",
                  "icon"        : false,
                  "action": function (obj) {
                      $scope.switchDisable();
                  }
                  ,"_disabled" : $scope.readonly
                }
					    }
					  }
					}
				  });
      tree = $('#jstree_demo_div').jstree(true);
      
      function getArtefactById(artefact, id) {
        if(artefact.id == id) {
          return artefact;
        } else {
          var children = artefact.children;
          if(children) {
            for(var i=0; i<children.length; i++) {
              var child = children[i];
              var result = getArtefactById(child, id);
              if(result != null) {
                return result;
              }
            }
          }
          return null;
        }
      }
      
      $('#jstree_demo_div').on('changed.jstree', function (e, data) {
      	var selectedNodes = tree.get_selected(true);
      	if(selectedNodes.length > 0) {
      	  $scope.selectedArtefact = getArtefactById($scope.plan.root, selectedNodes[0].id);
      	}
      	$scope.$apply();
      })
      
      $(document).on("dnd_move.vakata", function (e, data) {
        //Triggered continuously during drag 
      }).bind("dnd_stop.vakata", function(e, data) { //Triggered on drag complete
          if ($scope.nodesToMove !== undefined && $scope.nodesToMove.length > 0) {
            _.each($scope.nodesToMove, function(node) {
              var id = node.id;
              var artefact = getArtefactById($scope.plan.root, node.id);
              var oldParent = getArtefactById($scope.plan.root, node.oldParent);
              var newParent = getArtefactById($scope.plan.root, node.parent);
              oldParent.children = _.reject(oldParent.children, function(child) {
                return child.id == id;
              });
              newParent.children.splice(node.position, 0, artefact);
              reloadAfterArtefactInsertion(artefact);
              $scope.fireChangeEvent();
              $scope.nodesToMove=[];
            });
          }
      });
      
      //Triggered for each node moved before the dnd_stop.vakata event
      $('#jstree_demo_div').on("move_node.jstree", function (e, data) {
        if ($scope.nodesToMove === undefined) {
          $scope.nodesToMove = [];
        }
        var node = {
            "id" : data.node.id,
            "text" : data.node.text,
            "oldParent" : data.old_parent,
            "parent" : data.parent,
            "position" : data.position
        }
        $scope.nodesToMove.push(node);
      })
      
      $("#jstree_demo_div").delegate("a","dblclick", function(e) {
        $scope.openSelectedArtefact();
      });

      $('#jstree_demo_div').on('keydown.jstree', '.jstree-anchor', function (e, data) {
        //Only react on keyboard while not renaming a node
        if (!$scope.renaming) {
          if(e.which === 46 && !$scope.readonly) {
            e.preventDefault(); 
            $scope.remove();
          }
          else if(e.which === 67 && (e.ctrlKey || e.metaKey) && !$scope.readonly) {
            e.preventDefault(); 
            $scope.copy();
          }
          else if(e.which === 86 && (e.ctrlKey || e.metaKey) && !$scope.readonly) {
            e.preventDefault(); 
            $scope.paste();
          }
          else if (e.which === 38 && (e.ctrlKey || e.metaKey) && !$scope.readonly) {
            $scope.move(-1);
            e.stopImmediatePropagation();
            e.preventDefault();
          }
          else if (e.which === 40 && (e.ctrlKey || e.metaKey) && !$scope.readonly) {
            $scope.move(1);
            e.stopImmediatePropagation();
            e.preventDefault();
          }
          else if (e.which === 79 && (e.ctrlKey || e.metaKey)) {
            $scope.openSelectedArtefact();
            e.stopImmediatePropagation();
            e.preventDefault();
          } else if (e.which === 69 && (e.ctrlKey || e.metaKey) && !$scope.readonly) {
            $scope.switchDisable();
            e.stopImmediatePropagation();
            e.preventDefault();
          }
          else if (e.which === 113 && !$scope.readonly) {
            $scope.rename();
          }
          else if (e.which === 13 && (e.ctrlKey || e.metaKey) && $scope.isInteractiveSessionActive()) {
            $scope.execute();
            e.stopImmediatePropagation();
            e.preventDefault();
          }
        }
      })

      function getNodeLabel(artefact) {
        var label = "Unnamed";
        if(artefact.attributes && artefact.attributes.name) {
          label = artefact.attributes.name
        }
        return label;
      }
      
      function load(callback) {
  	  	treeData = [];
      	function asJSTreeNode(currentNode) {
      	  var children = [];
      	  _.each(currentNode.children, function(child) {
      	    children.push(asJSTreeNode(child))
      	  }) 	  
      	  
      	  var artefact = currentNode;

      	  var icon = artefactTypes.getIcon(artefact._class);
      	  
      	  var node = { "id" : artefact.id, "children" : children, "text" : getNodeLabel(artefact), icon:"glyphicon "+icon, data: {"artefact": artefact} }
      	  if (artefact.skipNode.value) {
      	    node.li_attr = { "class" : "text-muted" }
      	  }
      	  
      	  if (artefact._class === 'CallPlan') {
      	    node.planId = artefact.id;
      	  } else if (artefact._class === 'CallKeyword') {
      	    node.callFunctionId = artefact.id; 
      	  }
      	  
      	  return node;
      	}
      	
      	var root = asJSTreeNode($scope.plan.root);
        
      	treeData.push(root)
      	tree.settings.core.data = treeData;
      	
      	$('#jstree_demo_div').one("refresh.jstree", function() {
      		if(callback) {
      			callback(root); 		
      		}
      	})

      	tree.refresh();
      }
      
      function focusOnNode(nodeId) {
        var node = tree.get_node(nodeId, true);
        if (typeof node.children === "function" && (child = node.children('.jstree-anchor')) !== "undefined") { 
          child.focus();
        }
      }
      
      function reloadAfterArtefactInsertion(artefact) {
      	load(function() {
    			tree.deselect_all(true);
    			tree._open_to(artefact.id);
    			tree.select_node(artefact.id);    			
    			focusOnNode(artefact.id);
    		});  
      }
      
      function addArtefactToCurrentNode(newArtefact) {
        var selectedArtefact = getSelectedArtefact();
        selectedArtefact.children.push(newArtefact);
        reloadAfterArtefactInsertion(newArtefact)
        $scope.fireChangeEvent();
      }
      
      if($scope.handle) {
        $scope.handle.addFunction = function(id) {
          var selectedArtefact = tree.get_selected(true);
          
          $http.get("rest/functions/"+id).then(function(response) {
            var function_ = response.data;
    
            $http.get("rest/controller/artefact/types/CallKeyword").then(function(response) {
              var newArtefact = response.data;
              newArtefact.attributes.name=function_.attributes.name;
              newArtefact.functionId=function_.id;
              
              if(AuthService.getConf().miscParams.enforceschemas === 'true'){
                var targetObject = {};
                
                if(function_.schema && function_.schema.properties){
                  _.each(Object.keys(function_.schema.properties), function(prop) {
                    var value = "notype";
                    if(function_.schema.properties[prop].type){
                      var propValue = {};
                      value = function_.schema.properties[prop].type;
                      if(value === 'number' || value === 'integer')
                        propValue = {"expression" : "<" + value + ">", "dynamic" : true};
                      else
                        propValue = {"value" : "<" + value + ">", "dynamic" : false};
                      
                      targetObject[prop] = propValue;
                    }
                  });
                  
                  if(function_.schema.required) {
                    _.each(function_.schema.required, function(prop) {
                      if(targetObject[prop] && targetObject[prop].value)
                        targetObject[prop].value += " (REQ)";
                      if(targetObject[prop] && targetObject[prop].expression)
                        targetObject[prop].expression += " (REQ)";
                    });
                  }
                  
                  newArtefact.argument = {  
                      "dynamic":false,
                      "value": JSON.stringify(targetObject),
                      "expression":null,
                      "expressionType":null
                  }
                }
              }
              
              addArtefactToCurrentNode(newArtefact)
            });
          });
        }
        
        $scope.handle.addControl = function(id) {
          $http.get("rest/controller/artefact/types/"+id).then(function(response) {
            var artefact = response.data;
            addArtefactToCurrentNode(artefact);
          });
        }
        
        $scope.handle.addPlan = function(id) {
          $http.get("rest/plans/"+id).then(function(response) {
            var plan = response.data;
            $http.get("rest/controller/artefact/types/CallPlan").then(function(response) {
              var newArtefact = response.data;
              newArtefact.attributes.name=plan.attributes.name;
              newArtefact.planId=id;
              addArtefactToCurrentNode(newArtefact);
            });
            
          });
        }

        $scope.handle.undo = function() {
          if ($scope.undoStack.length > 1) {
            $scope.redoStack.push($scope.undoStack.pop());
            $scope.plan = $scope.undoStack.pop();
            $scope.fireChangeEvent(true);
          }
        }

        $scope.handle.discardAll = function () {
          while ($scope.undoStack.length > 1) {
            $scope.redoStack.push($scope.undoStack.pop());
          }
          if ($scope.undoStack.length > 0) {
            $scope.plan = $scope.undoStack.pop();
            $scope.fireChangeEvent(true);
          }
        }

        $scope.handle.hasUndo = function () {
          return ($scope.undoStack.length > 1);
        }

        $scope.handle.redo = function() {
          if ($scope.redoStack.length > 0) {
            $scope.plan = $scope.redoStack.pop();
            $scope.fireChangeEvent(true);
          }
        }

        $scope.handle.hasRedo = function () {
          return ($scope.redoStack.length > 0);
        }
      }
      
      
      $scope.openSelectedArtefact = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        if (selectedArtefact.original.planId) {
          $http.get('rest/plans/'+$scope.plan.id+'/artefacts/'+selectedArtefact.id+'/lookup/plan').then(function(response) {
            if (response.data) {
              var planId = response.data.id
              if (planId) {
                openPlan(planId);
              } else {
                Dialogs.showErrorMsg("No editor configured for this plan type");
              }
            } else {
              Dialogs.showErrorMsg("The related plan was not found");
            }
          });
        } else if (selectedArtefact.original.callFunctionId) {
          var artefact = getSelectedArtefact();
          $http.post("rest/functions/lookup",artefact).then(function(response) {
            if (response.data) {
              var planId = response.data.planId;
              if (planId) {
                openPlan(planId);
              } else {
                Dialogs.showErrorMsg("No editor configured for this function type");
              }
            } else {
              Dialogs.showErrorMsg("The related keyword was not found");
            }
          });
        }
      }
      
      $scope.isNodeDisabled = function() {
        var selectedNode = tree.get_selected(true)[0];
        var selectedArtefact = getArtefactById($scope.plan.root, selectedNode.id);
        return selectedArtefact.skipNode.value;
      }
      
      $scope.switchDisable = function() {
        var selectedNode = tree.get_selected(true)[0];
        var selectedArtefact = getArtefactById($scope.plan.root, selectedNode.id);
        selectedArtefact.skipNode.value = !selectedArtefact.skipNode.value;
        selectedNode.li_attr = (selectedArtefact.skipNode.value) ? { "class" : "text-muted" } : {"class" : ""}
        $scope.fireChangeEvent();
        tree.redraw(true);
      }
      
      function getSelectedArtefact() {
        var selectedNode = tree.get_selected(true)[0];
        var selectedArtefact = getArtefactById($scope.plan.root, selectedNode.id);
        return selectedArtefact;
      }
      
      openPlan = function(planId) {
        $timeout(function() {
          $location.path('/root/plans/editor/' + planId);
        });
      }
      
      $scope.rename = function() {
        $scope.renaming=true;
        var selectedNode = tree.get_selected(true)[0];
        tree.edit(selectedNode.id, selectedNode.text, function (selectedNode, status, cancelled) {
          if (!selectedNode.text || !status || cancelled) {
            //skip
          } else {
            var selectedArtefact = getSelectedArtefact();
            selectedArtefact.attributes.name = selectedNode.text;
            load(function () {
              focusOnNode(selectedArtefact.id);
            });
          }
          $scope.renaming=false;
          $scope.fireChangeEvent();
        });
      }
      
      $scope.copy = function() {
        var selectedArtefact = getSelectedArtefact();
        $rootScope.clipboard = {object:"artefact",artefact:selectedArtefact};
      }
      
      $scope.paste = function() {
        var selectedArtefact = getSelectedArtefact();
        if($rootScope.clipboard && $rootScope.clipboard.object=="artefact") {
          $http.post("rest/plans/artefacts/clone", $rootScope.clipboard.artefact).then(function(response) {
            var clone = response.data;
            selectedArtefact.children.push(clone)
            load(function () {
              focusOnNode(clone.id);
            });
            $scope.fireChangeEvent();
          });
        }
      }
      
      $scope.remove = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        var parentid = tree.get_parent(selectedArtefact);
        var previousNode = tree.get_prev_dom(selectedArtefact.id);
        
        var parentArtefact = getArtefactById($scope.plan.root, parentid);
        parentArtefact.children = _.reject(parentArtefact.children, byId(selectedArtefact.id))
        
        load(function () {
          setSelectedNode(previousNode);
        });
        $scope.fireChangeEvent();
      }
      
      function byId(id) {
        return function(artefact) {
          return artefact.id == id;
        }
      }
      
      $scope.move = function(offset) {
        var selectedNode = tree.get_selected(true)[0];
      	var parentid = tree.get_parent(selectedNode);

      	var selectedArtefact = getArtefactById($scope.plan.root, selectedNode.id);
      	var parentArtefact = getArtefactById($scope.plan.root, parentid);
      	var children = parentArtefact.children;
      	
      	var index = _.findIndex(children, byId(selectedArtefact.id));
      	var newIndex = index + offset;
      	
      	if(newIndex>=0 && newIndex<children.length) {
      	  var temp = children[newIndex] 
      	  children[newIndex] = selectedArtefact
      	  children[index] = temp
      	  
      	  load(function () {
      	    focusOnNode(selectedArtefact.id);
      	  })
      	  $scope.fireChangeEvent();
      	}
      }
      
      $scope.isInteractiveSessionActive = function() {
      	return $scope.interactiveSessionHandle.id!=null;
      }
      
      $scope.execute = function() {
        var selectedArtefact = tree.get_selected(true)[0];
        $scope.interactiveSessionHandle.execute(selectedArtefact);
      }
      
      $scope.onSelectedArtefactSave = function(artefact) {
        var currentNode = tree.get_node(artefact.id);
        if(currentNode) {
          var currentLabel = tree.get_text(currentNode);
          var newLabel = getNodeLabel(artefact);
          if(newLabel!=currentLabel) {
            tree.rename_node(currentNode,newLabel);
          } 
          currentNode.li_attr = (artefact.skipNode.value) ? { "class" : "text-muted" } : {"class" : ""}
          $timeout(function(){tree.redraw(true)});
          $scope.fireChangeEvent();
        } else {
          console.error("Unable to find not with id: "+artefact.id);
        }
      }
      
      $scope.fireChangeEvent = function(keepRedoStack) {
        if($scope.stOnChange) {
          $scope.stOnChange();
        }
        var backupPlan = JSON.parse(JSON.stringify($scope.plan));
        $scope.undoStack.push(backupPlan);
        if (keepRedoStack == undefined && !keepRedoStack) {
          $scope.redoStack = [];
        }
      }
      
    },
    templateUrl: 'partials/plans/planTree.html'}
})