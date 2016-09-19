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
var Measurement = Backbone.Model.extend({});

var Measurements = Backbone.Collection.extend({
      //url: '/rtm/rest/service/allmeasurements',
      url: '/rtm/rest/service/measurement',
      model: Measurement,

      refreshData: function(rawInput){

        var that = this;

        input = JSON.stringify(rawInput);
        //console.log('Fetching Measurements with following input : ' + input);
        this.fetch({
          type : 'POST',
          dataType:'json',
          contentType:'application/json; charset=utf-8',
          data: input,
          success: function (response) {
            //console.log('loaded ' + response.length + ' objects.');
            //console.log(response);
            that.trigger('MeasurementsRefreshed');
          },
          error: function( model, response, options ){
            //console.log('model=' + JSON.stringify(model) + ', response=' + JSON.stringify(response) + ', options=' + JSON.stringify(options));
          	 displayError('[SERVER_CALL] Technical Error=' + JSON.stringify(response)+ ';       input=' + input);
            that.reset();
          }
        });
        return status;
      }
    });
