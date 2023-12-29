import { StyleSheet, Text, View, NativeModules, Alert, Button, TextInput } from 'react-native'
import React, { useEffect, useState } from 'react'
const { FridaModule } = NativeModules;
// import RNFS from 'react-native-fs';

const App = () => {
  const [Connect, setWebConnect] = useState<any>(false);
  const [title, setTitle] = useState('');
  const [data, setData] = useState('ws://139.135.52.82:8003');
  const [value, setValue] = useState('');
  var ws: any;


  useEffect(() => {
    // FridaModule.executeTerminalCommand('mkdir newfolder').then((result: any) => {
    //   console.log(result); // Output: "Command executed successfully"
    // })
    //   .catch((error: any) => {
    //     console.error(error);
    //   });
    // const web = new WebSocket('ws://139.135.52.82:8003');
    // setWebConnect(web);
    console.log("FridaModule------------", FridaModule)
  }, []);
  useEffect(() => {
    ws = new WebSocket(data);

    ws.onopen = () => {
      // You can send messages after the connection is open
      ws.send(JSON.stringify({ message: 'Hello, WebSocket Server!' }));
      setWebConnect(true);
    };

    ws.onmessage = async (event: any) => {
      console.log('Received message:', event);
      const data = JSON.parse(event.data);
      if (data?.event === "attestation") {
        FridaModule.handleAttestation(data?.data.device_id, data?.data.nonce, (response: any) => {
          ws.send(response);
        });

      } else if (data?.event === "sign") {
        if (data?.data.signature_inputs["/ValidateChallenge"] !== undefined) {
          FridaModule.handleSign(data?.data.device_id, data?.data.signature_inputs["/ValidateChallenge"], (response: any) => {
            ws.send(JSON.stringify({
              validateChallange: "true",
              data: response
            }));
          });
        } else {
          FridaModule.handleSign(data?.data.device_id, data?.data.signature_inputs["/AcceptOffer"], (response: any) => {
            ws.send(JSON.stringify({
              acceptOffer: "false",
              data: response
            }));
          });
        }
      }
    };

    ws.onerror = (error: any) => {
      console.error('WebSocket error:', error);
      // Handle WebSocket errors here
    };

    ws.onclose = () => {
      console.log('WebSocket closed!');
      setWebConnect(false);
      // setWebConnect(null);
      // Handle WebSocket close event here
    };

    // Cleanup function when component unmounts
    return () => {
      ws.close();
    };
  }, [Connect]);
  return (
    <View style={styles.conatiner}>
      {Connect ? <Text>Connected</Text> : <Text>Disconnected</Text>}
      <TextInput placeholder='Enter Ip' onChangeText={(text) => { setData(text) }} value={data} style={{ borderWidth: 1, width: '90%', textAlign: 'center' }} />
      {Connect ? null : <Button title='Connect' onPress={() => { setWebConnect(true) }} />}
    </View>
  )
}

export default App

const styles = StyleSheet.create({
  conatiner: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center'
  }
})
