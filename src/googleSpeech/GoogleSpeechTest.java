package googleSpeech;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

public class GoogleSpeechTest {
	
	public GoogleSpeechTest() {
		
		//Set credentials?
		//GoogleCredentials credentials = GoogleCredentials.create(new AccessToken("AIzaSyC3wjxzf3ay5vFfUVDBCI0H7B-i-Eqm5-A",Date.valueOf(LocalDate.now())));
		//System.out.print(credentials.getAccessToken());
		
		//Target data line
		TargetDataLine microphone;
		AudioInputStream audio = null;
		
		//Check if Microphone is Supported
		checkMicrophoneAvailability();
		
		//Print available mixers
		//printAvailableMixers();
		
		//Capture Microphone Audio Data
		try {
			
			// Signed PCM AudioFormat with 16kHz, 16 bit sample size, mono
			AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			
			//Check if Microphone is Supported
			if (!AudioSystem.isLineSupported(info)) {
				System.out.println("Microphone is not available");
				System.exit(0);
			}
			
			//Get the target data line
			microphone = (TargetDataLine) AudioSystem.getLine(info);
			microphone.open(format);
			microphone.start();
			
			//Audio Input Stream
			audio = new AudioInputStream(microphone);
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//Send audio from Microphone to Google Servers and return Text
		try (SpeechClient client = SpeechClient.create()) {
			
			ResponseObserver<StreamingRecognizeResponse> responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {
				
				public void onStart(StreamController controller) {
					System.out.println("Started....");
				}
				
				public void onResponse(StreamingRecognizeResponse response) {
					System.out.println(response.getResults(0));
				}
				
				public void onComplete() {
					System.out.println("Complete");
				}
				
				public void onError(Throwable t) {
					System.err.println(t);
				}
			};
			
			ClientStream<StreamingRecognizeRequest> clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);
			
			RecognitionConfig recConfig = RecognitionConfig.newBuilder().setEncoding(RecognitionConfig.AudioEncoding.LINEAR16).setLanguageCode("en-US").setSampleRateHertz(16000)
					.build();
			StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder().setConfig(recConfig).build();
			
			StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder().setStreamingConfig(config).build(); // The first request in a streaming call has to be a config
			
			clientStream.send(request);
			
			//Infinity loop from microphone
			while (true) {
				byte[] data = new byte[10];
				try {
					audio.read(data);
				} catch (IOException e) {
					System.out.println(e);
				}
				request = StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data)).build();
				clientStream.send(request);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		
	}
	
	/**
	 * Checks if the Microphone is available
	 */
	public static void checkMicrophoneAvailability() {
		enumerateMicrophones().forEach((string , info) -> {
			
			System.out.println("Name :" + string);
		});
	}
	
	/**
	 * Generates a hashmap to simplify the microphone selection process. The keyset is the name of the audio device's Mixer The value is the first
	 * lineInfo from that Mixer.
	 * 
	 * @author Aaron Gokaslan (Skylion)
	 * @return The generated hashmap
	 */
	public static HashMap<String,Line.Info> enumerateMicrophones() {
		HashMap<String,Line.Info> out = new HashMap<String,Line.Info>();
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info info : mixerInfos) {
			Mixer m = AudioSystem.getMixer(info);
			Line.Info[] lineInfos = m.getTargetLineInfo();
			if (lineInfos.length >= 1 && lineInfos[0].getLineClass().equals(TargetDataLine.class))//Only adds to hashmap if it is audio input device
				out.put(info.getName(), lineInfos[0]);//Please enjoy my pun
		}
		return out;
	}
	
	/**
	 * Print available mixers
	 */
	public void printAvailableMixers() {
		
		//Get available Mixers
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		
		//Print available Mixers
		Arrays.asList(mixerInfos).forEach(info -> {
			System.err.println("\n-----------Mixer--------------");
			
			Mixer mixer = AudioSystem.getMixer(info);
			
			System.err.println("\nSource Lines");
			
			//SourceLines
			Arrays.asList(mixer.getSourceLineInfo()).forEach(lineInfo -> {
				//Line Name
				System.out.println(info.getName() + "---" + lineInfo);
				Line line = null;
				try {
					line = mixer.getLine(lineInfo);
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("\t-----" + line);
			});
			
			System.err.println("\nTarget Lines");
			//TargetLines
			Arrays.asList(mixer.getTargetLineInfo()).forEach(lineInfo -> {
				
				//Line Name
				System.out.println(mixer + "---" + lineInfo);
				Line line = null;
				try {
					line = mixer.getLine(lineInfo);
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("\t-----" + line);
				
			});
			
		});
	}
	
	public static void main(String[] args) {
		new GoogleSpeechTest();
	}
	
}
