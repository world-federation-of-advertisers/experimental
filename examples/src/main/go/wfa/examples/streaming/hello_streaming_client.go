/*
 * Copyright 2021 The Cross-Media Measurement Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package main

import (
	"context"
	"fmt"
	"google.golang.org/grpc"
	"log"
	"time"
	pb "wfa/examples/streaming/grpc"
)

func main() {
	serverPort := "50051"
	address := fmt.Sprintf("localhost:%s", serverPort)
	conn, err := grpc.Dial(address, grpc.WithInsecure())
	if err != nil {
		log.Fatalf("fail to dial: %v", err)
	}
	defer conn.Close()
	client := pb.NewHelloStreamingClient(conn)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()
	stream, err := client.SayHelloStreaming(ctx)
	names := []string{"Alice", "Bob", "Carol"}
	for _, name := range names {
		err = stream.Send(&pb.SayHelloStreamingRequest{Name: name})
		if err != nil {
			log.Fatalf("fail to send: %v", err)
		}
	}
	response, err := stream.CloseAndRecv()
	if err != nil {
		log.Fatalf("failed stream: %v", err)
	}
	for _, message := range response.GetMessage() {
		log.Printf("Received: %s", message)
	}
}
