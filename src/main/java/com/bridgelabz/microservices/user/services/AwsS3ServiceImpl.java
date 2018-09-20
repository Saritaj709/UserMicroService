package com.bridgelabz.microservices.user.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Service
public class AwsS3ServiceImpl implements AwsS3Service {

	@Autowired
	private Environment environment;

	private static final String SUFFIX = "/";

	@Override
	public String createBucket(String bucketName) {
		// create bucket - name must be unique for all S3 users
		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));

		AmazonS3 s3client = new AmazonS3Client(credentials);
		s3client.createBucket(bucketName);
		return bucketName;
	}

	@Override
	public List<Bucket> showBucket() {
		// list buckets

		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));
		AmazonS3 s3client = new AmazonS3Client(credentials);
		for (Bucket bucket : s3client.listBuckets()) {
			System.out.println(" - " + bucket.getName());
		}
		return s3client.listBuckets();
	}

	@Override
	public String uploadFile(String bucketName, String folderName, String file) {
		// upload file to folder and set it to public
		String fileName = folderName + SUFFIX + file;
		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));
		AmazonS3 s3client = new AmazonS3Client(credentials);

		s3client.putObject(
				new PutObjectRequest(bucketName, fileName, new File(environment.getProperty("sourceFolder") + file))
						.withCannedAcl(CannedAccessControlList.PublicRead));
		return fileName;
	}

	@Override
	public String deleteBucket(String bucketName) {
		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));
		AmazonS3 s3client = new AmazonS3Client(credentials);
		s3client.deleteBucket(bucketName);
		return bucketName;
	}

	@Override
	public String createFolderInBucket(String bucketName, String folderName) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);
		InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName + SUFFIX, emptyContent,
				metadata);
		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));
		AmazonS3 client = new AmazonS3Client(credentials);
		client.putObject(putObjectRequest);
		return folderName;
	}

	/**
	 * This method first deletes all the files in given folder and than the folder
	 * itself
	 */
	@Override
	public String deleteFolderAndFile(String bucketName, String folderName) {
		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));
		AmazonS3 client = new AmazonS3Client(credentials);
		List<S3ObjectSummary> fileList = client.listObjects(bucketName, folderName).getObjectSummaries();
		for (S3ObjectSummary file : fileList) {
			client.deleteObject(bucketName, file.getKey());
		}
		client.deleteObject(bucketName, folderName);
		return folderName;
	}

	@Override
	public String deleteImagesFromNote(String image) {
		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));
		AmazonS3 client = new AmazonS3Client(credentials);
		List<S3ObjectSummary> fileList = client
				.listObjects(environment.getProperty("bucketName"), environment.getProperty("folderNameForNote"))
				.getObjectSummaries();
		for (S3ObjectSummary file : fileList) {
			if (file.getKey()
					.equals(environment.getProperty("folderNameForNote")+SUFFIX + image))
				client.deleteObject(environment.getProperty("bucketName"), file.getKey());
		}
		return image;
	}
	
	@Override
	public String deleteImagesFromUser(String image) {
		AWSCredentials credentials = new BasicAWSCredentials(environment.getProperty("aws_access_key_id"),
				environment.getProperty("aws_secret_access_key"));
		AmazonS3 client = new AmazonS3Client(credentials);
		List<S3ObjectSummary> fileList = client
				.listObjects(environment.getProperty("bucketName"), environment.getProperty("folderNameForUser"))
				.getObjectSummaries();
		for (S3ObjectSummary file : fileList) {
			if (file.getKey()
					.equals(environment.getProperty("folderNameForUser")+SUFFIX + image))
				client.deleteObject(environment.getProperty("bucketName"), file.getKey());
		}
		return image;
	}
}
