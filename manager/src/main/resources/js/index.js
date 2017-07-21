$(document).ready(function () {

    //Test Case Table
    var testcase_table = $('#testcase-table').DataTable({
        dom: 'frBtlip',
        buttons: [
            'selectAll',
            'selectNone',
            {
                text: "Run selected entries",
                action: function (e, dt, node, config) {

                    // send configuration info
                    sendConfig();
                    var queueArray = new Array();
                    dt.rows({selected: true}).every(function (rowIdx, tableLoop, rowLoop) {
                        queueArray.push(this.data().casenum);
                    });

                    $.ajax({
                        url: "/json/testqueue/post",
                        type: "POST",
                        data: queueArray.toString(),
                        dataType: "text",
                        contentType: "text/plain",
                        async: false,
                        success: function (data) {
                            alert(data);
                        },
                        error: function (xhr, ajaxOptions, thrownError) {
                            alert(xhr.status);
                            alert(thrownError);
                        }

                    });
                }
            }
        ],
        select: {
            style: 'multi'
        },
        lengthMenu: [50, 100],
        searching: true,
        'columns': [
            {'data': 'category'},
            {'data': 'casenum'},
            {'data': 'name'},
        ],
        'ajax': {
            'url': '/json/testcases',
            'dataSrc': ""
        },
        "createdRow": function (row, data) {
            $(row).attr('title', data['desc']);
            $(row).tooltip({
                "delay": 0,
                "track": true,
                "fade": 250
            });
        },
        "order": [[0, "asc"], [1, "asc"]],
    });

    //Queue Table
    var queue_table = $('#queue-table').DataTable({
        dom: 'frBtlip',
        buttons: [
            {
                text: "Remove selected entries",
                action: function (e, dt, node, config) {

                    var queueArray = new Array();
                    dt.rows({selected: true}).every(function (rowIdx, tableLoop, rowLoop) {
                        if (this.data().status != 'COMPLETE') {
                            queueArray.push(this.data().index);
                        }
                    });

                    $.ajax({
                        url: "/json/testqueue/stop",
                        type: "POST",
                        data: queueArray.toString(),
                        dataType: "text",
                        contentType: "text/plain",
                        async: false,
                        success: function (data) {
                            alert(data);
                        },
                        error: function (xhr, ajaxOptions, thrownError) {
                            alert(xhr.status);
                            alert(thrownError);
                        }

                    });
                }
            }
        ],
        select: {
            style: 'multi'
        },
        lengthMenu: [20, 50, 100],
        searching: true,
        'columns': [
            {'data': 'index'},
            {'data': 'time'},
            {'data': 'category'},
            {'data': 'casenum'},
            {'data': 'name'},
            {'data': 'status'},
            {'data': 'result'}
        ],
        'ajax': {
            'url': '/json/testqueue/get',
            'dataSrc': ""
        },
        "fnRowCallback": function (nRow, aData) {
            if (aData['status'] == "RUNNING") {
                $('td', nRow).css('background-color', '#ffff99');
            }
            else if (aData['status'] == "COMPLETE") {
                if (aData['result'] == "FAIL") {
                    $('td', nRow).css('background-color', '#ff9999');
                }
                else if (aData['result'] == "PASS") {
                    $('td', nRow).css('background-color', '#ccff99');
                }
            }
        },
        "createdRow": function (row, data) {
            $(row).attr('title', data['desc']);
            $(row).tooltip({
                "delay": 0,
                "track": true,
                "fade": 250
            });
        },
    });

    setInterval(function () {
        queue_table.ajax.reload();
    }, 4000);

    setInterval(function () {

        $.ajax({
            url: '/text/getlog',
            dataType: 'text',
            contentType: "text/plain",
            async: false,
            success: function (data) {
                var old = $('#deltalog').val();
                if (old.length != data.length) {
                    $('#deltalog').val(data);
                    $('#deltalog').scrollTop($('#deltalog')[0].scrollHeight);
                }
            }
        });
    }, 1000);

});

// $.ajax({
//     url: '/text/getconfig',
//     dataType: 'text',
//     contentType: "text/plain",
//     async: false,
//     success: function (data) {
//         $('#configpane').val(data);
//         $('#configpane').scrollTop($('#configpane')[0].scrollHeight);
//     }
// });

//change handler version select options according to handler controller

$('#targetController').change(function() {
    $('#targetVersion').empty();
    targetController = $('#targetController').val();
    if (targetController == 'ONOS') {
        $('#targetVersion').append('<option value="1.9.0">1.9.0</option>');
        $('#targetVersion').append('<option value="1.6.0">1.6.0</option>');
        $('#targetVersion').append('<option value="1.1.0">1.1.0</option>');
    } else if (targetController == 'OpenDaylight') {
        $('#targetVersion').append('<option value="helium">helium</option>');
        $('#targetVersion').append('<option value="carbon">carbon</option>');
    } else if (targetController == 'Floodlight') {
        $('#targetVersion').append('<option value="1.2">1.2</option>');
        $('#targetVersion').append('<option value="0.91">0.91</option>');
    }
    $("#targetVersion").selectpicker("refresh");
});

// send configuration info to the server from UI

function sendConfig() {

    var configMsg = $('#targetController').val() + " " + $('#targetVersion').val() + " " + $('#ofPort').val() + " "
         + $('#ofVersion').val() + " " + $('#controllerIp').val() + " " + $('#switchIp').val() + " " + $('#topologyType').val();

    $.ajax({
        url: "/json/config/post",
        type: "POST",
        data: configMsg,
        dataType: "text",
        contentType: "text/plain",
        async: false,
        success: function (data) {
            // alert(data);
        },
        error: function (xhr, ajaxOptions, thrownError) {
            alert(xhr.status);
            alert(thrownError);
        }
    });
}
