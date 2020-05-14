/*
 * Downloaded from the example in the documentation at:
 *    http://datatables.net/release-datatables/extras/FixedColumns/row_grouping_height.html
 */

$(document).ready( function () {
    var tableID = "#endpoints";
    var oTable = $(tableID).dataTable( {
        "sScrollY": "100%",
        "sScrollX": "100%",
        "sScrollXInner": "100%",
        "bScrollCollapse": true,
        "destroy": true,
        "bPaginate": true,
        "order": [[0, "asc"], [1, "asc"]],
        // pageLength: -1,
        processing: true,
        "aLengthMenu": [[10,25,50,100,250,500,-1], [10,25,50,100,250,500,"ALL"]]
    });

    //$(tableID).dataTable().columns([3]).visible(false);

    new FixedColumns( oTable, {
        "fnDrawCallback": function ( left, right ) {
            var that = this, groupVal = null, matches = 0, heights = [], index = -1;
            
            /* Get the heights of the cells and remove redundant ones */
            $('tbody tr td', left.body).each( function ( i ) {
                var currVal = this.innerHTML;
                
                /* Reset values on new cell data. */
                if (currVal != groupVal) {
                    groupVal = currVal;
                    index++;
                    heights[index] = 0;
                    matches = 0;
                } else  {
                    matches++;
                }
                
                heights[ index ] += $(this.parentNode).height();
                if ( currVal == groupVal && matches > 0 ) {
                    this.parentNode.parentNode.removeChild(this.parentNode);
                }
            } );

            /* Now set the height of the cells which remain, from the summed heights */
            $('tbody tr td', left.body).each( function ( i ) {
                that.fnSetRowHeight( this.parentNode, heights[i] );
            });
        }
    });
});