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
	"fmt"
	"google.golang.org/grpc"
	"io"
	"log"
	"net"
	pb "wfa/examples/streaming/grpc"
)

type helloStreamingServer struct {
	pb.UnimplementedHelloStreamingServer
}

func (s *helloStreamingServer) SayHelloStreaming(stream pb.HelloStreaming_SayHelloStreamingServer) error {
	names := []string{}
	for {
		request, err := stream.Recv()
		if err == io.EOF {
			return stream.SendAndClose(&pb.SayHelloStreamingResponse{Message: names})
		}
		if err != nil {
			return err
		}
		log.Printf("Received: %s", request.GetName())
		names = append(names, fmt.Sprintf("Hello %s", request.GetName()))
	}
}

func main() {
	address := "localhost:50051"
	lis, err := net.Listen("tcp", address)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	grpcServer := grpc.NewServer()
	pb.RegisterHelloStreamingServer(grpcServer, &helloStreamingServer{})
	log.Printf("Golang server listening: %s...", address)
	grpcServer.Serve(lis)
}
