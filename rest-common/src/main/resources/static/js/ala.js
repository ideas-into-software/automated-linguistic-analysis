/**
 * Automated Linguistic Analysis (ALA)
 * 
 * @author mhs, email: mhs@into.software, www: https://ideas.into.software/
 * @author Osvaldas Valutis, www.osvaldas.info (Drag and Drop upload support based on article @ https://css-tricks.com/drag-and-drop-file-uploading/)
 */

'use strict';

const statusesMap = new Map();
statusesMap.set('transcript_requested', "Transcript was requested");
statusesMap.set('transcript_pending', "Transcript is pending");
statusesMap.set('transcript_ready', "Transcript is ready!");
statusesMap.set('transcript_failed', "Transcript failed!");
statusesMap.set('analysis_requested', "Analysis was requested");
statusesMap.set('analysis_pending', "Analysis is pending");
statusesMap.set('analysis_ready', "Analysis is ready!");
statusesMap.set('analysis_failed', "Analysis failed!");

function browserSupported() {
	$("#error_notsupported").hide();
	$("#drag_n_drop_upload").show();
	$("#choose-file").show(),
	$("#language_field").show();
	$("#description_field").show();
	$("#submit_button").show();
	$("#hint").show();
}

function displayStatusMessage(statusMessage) {
	$('#status_message').text(statusMessage);
	$("#statuses").show();
}

function displayProcessingErrorMessage(errorMessage) {
	$("#error_processing").text(errorMessage);
	$("#error_processing").show();
	
	displayTryAgain();
	clearStatusMessage();
}

function displayFileErrorMessage(errorMessage) {
	$("#error_file").text(errorMessage);
	$("#error_file").show();
}

function clearFileErrorMessage() {
	$("#error_file").hide();
}

function clearStatusMessage() {
	$("#statuses").hide();
}

function displayTranscript(transcript) {
	$("#transcript_content").val(transcript);
	$("#transcript").show();
}

function displayAnalysis(analysis) {
	$("#analysis_content").text(analysis);
	$("#analysis").show();
}

function displayAnalysisVisual(analysis) {	
	let analysisJson = JSON.parse(analysis);
	
	let tones = analysisJson.document_tone.tones;
	
	for (let i in tones) {
		let tone = tones[i];
		
		let toneId = tone.tone_id;
		
		let tonesUIContainer = $("#tones");
		
		let tonesUIElement = tonesUIContainer.find(`#tone_${toneId}`);
		
		tonesUIElement.addClass( `tone-${toneId}` ).removeClass( 'no-tone' );
	}
	
	$("#analysis").show();
}

function displayTryAgain() {
	$("#try_again").show();
}

function retrieveTranscript(fileId) {

	let request = $.ajax({
		url : `/voiceanalysis/${fileId}/transcript`,
		type : "GET",
		dataType : 'json',
		contentType : 'application/json;charset=UTF-8'
	});

	request.done(function(data, status) {
		let transcript = data.content;
		displayTranscript(transcript);
	});
	
	request.fail(function(xhr, status, error) {
		displayProcessingErrorMessage(error);
	});	
}

function retrieveAnalysis(fileId) {

	let request = $.ajax({
		url : `/voiceanalysis/${fileId}/analysis`,
		type : "GET",
		dataType : 'json',
		contentType : 'application/json;charset=UTF-8'
	});

	request.done(function(data, status) {
		let analysis = data.content;
		displayAnalysisVisual(analysis);
	});
	
	request.fail(function(xhr, status, error) {
		displayProcessingErrorMessage(error);
	});
}

function monitorStatus(fileId) {
	console.log("Monitoring processing status for file ID: " + fileId);

	let sseMonitor = $.SSE(`/status/${fileId}`, {
		onOpen : function(e) {
			console.log(e);
		},
		onEnd : function(e) {
			console.log(e);
		},
		onError : function(e) {
			console.log("Could not connect");
		},
		events : {
			end : function(e) { // default 'onEnd' does not get called ..
				console.log(e);
				this.close();
			},

			status : function(e) {
				console.log(e);

				let eventData = e.data;

				displayStatusMessage(statusesMap.get(e.data));

				if (eventData == 'transcript_ready') {
					retrieveTranscript(fileId);

				} else if (eventData == 'analysis_ready') {
					retrieveAnalysis(fileId);
				}
			}
		}
	});

	sseMonitor.start();
}

function submitForVoiceAnalysis(formData, monitor) {

	let request = $.ajax({
		url : '/voiceanalysis',
		type : "POST",
		data : formData,
		dataType : 'json',
		cache: false,
		contentType: false,
		processData: false
	});

	request.done(function(data, status) {
		if (data.fileId) {
			let fileId = data.fileId
			console.log("Created new file with ID: " + fileId);
			
			displayStatusMessage("File successfully submitted! Await processing status updates..");

			monitor(data.fileId);
			
		} else {
			displayProcessingErrorMessage("Error occured!");
		}
	});

	request.fail(function(xhr, status, error) {
		displayProcessingErrorMessage(error);
	});
}

( function( $, window, document, undefined ) {
	
	let isSupported = function() {
		let div = document.createElement( 'div' );
		return ( ( 'draggable' in div ) || ( 'ondragstart' in div && 'ondrop' in div ) ) && 'FormData' in window && 'FileReader' in window && 'Map' in window && 'EventSource' in window;
	}();
	
	if ( isSupported ) {
		browserSupported();
	} else {
		return false;
	}
	
	let $alaForm = $("#ala_form");
	let $fileInput = $alaForm.find( 'input[type="file"]' );
	let $acceptedMime = $fileInput.attr( 'accept' ).split(',');
	let $fileInputLabel = $alaForm.find( '.choose_file' );
	let $tryAgain = $alaForm.find( '.ala_form__restart' );
	
	let droppedFile = false;
	
	let fileIsValid = function ( file ) {
		return $acceptedMime.includes( file.type );
	}
	
	let showFile = function( file ) {
		$fileInputLabel.text(file.name);
	};
	
	$fileInput.on( 'change', function( e ) {
		showFile( e.target.files[0] );
	});
	
	$alaForm.addClass( 'has-advanced-upload' )
	.on( 'drag dragstart dragend dragover dragenter dragleave drop', function( e ){
		e.preventDefault();
		e.stopPropagation();
	})
	.on( 'dragover dragenter', function() {
		$alaForm.addClass( 'is-dragover' );
	})
	.on( 'dragleave dragend drop', function() {
		$alaForm.removeClass( 'is-dragover' );
	})
	.on( 'drop', function( e ) {
		droppedFile = e.originalEvent.dataTransfer.files[0];
		
		clearFileErrorMessage();
		
		if (fileIsValid( droppedFile )) {
			showFile( droppedFile );
		} else {
			displayFileErrorMessage("Invalid file type!");
		}
	});
	
	$alaForm.on( 'submit', function( e ) {
		
		if( !droppedFile ) {
			displayFileErrorMessage("File is required!");
			return false;
		}
		
		displayStatusMessage("Uploading..");

		e.preventDefault();		
		
		let formData = new FormData( $alaForm.get( 0 ) );
		formData.set( $fileInput.attr( 'name' ), droppedFile );
		formData.append('fileFormat', droppedFile.type);
		
		$alaForm.find( 'input' ).prop("disabled", true);
		$alaForm.find( 'select' ).prop("disabled", true);
		$alaForm.find( 'textarea' ).prop("disabled", true);
		$alaForm.find( 'button[type="submit"]' ).prop("disabled", true);
		
		submitForVoiceAnalysis(formData, monitorStatus);
	});
	
	// Try again
	$tryAgain.on( 'click', function( e ) {
		e.preventDefault();
		location.reload(true);
	});	
	
	// Firefox focus bug fix for file input
	$fileInput
	.on( 'focus', function(){ $fileInput.addClass( 'has-focus' ); })
	.on( 'blur', function(){ $fileInput.removeClass( 'has-focus' ); });
	
})( jQuery, window, document );
