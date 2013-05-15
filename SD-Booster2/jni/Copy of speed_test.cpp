
// speed_test.cpp

// includes

#include <sys/types.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <time.h>
#include <fcntl.h>
#include <jni.h>
#include <android/log.h>

// typedefs

typedef unsigned int uint32;

//constants

static const char * className = "de/mehrmann/sdbooster/SpeedModell";
static const char * fileTag = "test.";
static const char * appTag = "SD-Booster";

// macros

#define LOG_I(x) (__android_log_print(ANDROID_LOG_INFO, appTag, (x)))
#define LOG_D(x) (__android_log_print(ANDROID_LOG_DEBUG, appTag, (x)))
#define LOG_E(x) (__android_log_print(ANDROID_LOG_ERROR, appTag, (x)))

// variables

struct field {
    const char * class_name;
    const char * field_name;
    const char * field_type;
    jfieldID   * jfield;
};

struct fields_t {
	jfieldID writeSpeed;
	jfieldID readSpeed;
	jfieldID usedTime;
} fields;

// prototypes

extern "C"  void Java_de_mehrmann_sdbooster_SpeedTest_runNative		(JNIEnv * env, jobject thiz, jobject modell, jstring cardPath);
extern "C"  void Java_de_mehrmann_sdbooster_SpeedTest_runOneNative	(JNIEnv * env, jobject thiz, jobject modell, jstring cardPath);
extern "C" 	jint JNI_OnLoad											(JavaVM * vm, void * reserved);
static bool find_fields												(JNIEnv * env, field * fields, int count);
static bool dir_exist                                            	(const char * path);
static void remove_files											(const char * path, const char * workDir, int usedFiles);

// functions

// JNI_OnLoad()

extern "C"
jint JNI_OnLoad(JavaVM * vm, void * reserved) {

	JNIEnv * env;

	if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
		LOG_E("GetEnv failed");
	    return -1;
	}

	field fields_to_find[] = {
	    { className, "writeSpeed",  "D",	&fields.writeSpeed },
	    { className, "readSpeed", 	"D", 	&fields.readSpeed },
	    { className, "usedTime", 	"I", 	&fields.usedTime },
	};

	if (!find_fields(env, fields_to_find, 3)) {
	    return -1;
	}

	return JNI_VERSION_1_6;
}

//runfNative()
extern "C"
void Java_de_mehrmann_sdbooster_SpeedTest_runfNative(JNIEnv * env, jobject thiz, jobject modell, jstring cardPath) {

	const char * path = (env)->GetStringUTFChars(cardPath, 0);
	const char * fileName = "test.0";
	const int size[11] = { 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576 };
	const int maxSpace = ((128 * 1024) * 1024);
	const int cluster = 4; //alignment
	const int readSize = 1024 * 1024;

	uint32 * ptr, random;
	struct timeval start, end;
	long mtime, secs, usecs;

	int i, j, k;
	char info[256];
	char workDir[32];
	double readSpeed = 0.0;
	double writeSpeed = 0.0;
	int freeSpace;
	int outBytes, inBytes;
	int usedData, usedTime;
	int randSize, randContent;

	if (!dir_exist(path)) {
		return;
	} else {
		strcpy(workDir, path);
		strcat(workDir, "/");
		strcat(workDir, appTag);
		chdir (workDir);
	}

	// init

	char * writeBuffer[7][cluster] = {
		{ (char *) malloc(size[0]), (char *) malloc(size[0]), (char *) malloc(size[0]), (char *) malloc(size[0]) },
		{ (char *) malloc(size[1]), (char *) malloc(size[1]), (char *) malloc(size[1]), (char *) malloc(size[1]) },
		{ (char *) malloc(size[2]), (char *) malloc(size[2]), (char *) malloc(size[2]), (char *) malloc(size[2]) },
		{ (char *) malloc(size[3]), (char *) malloc(size[3]), (char *) malloc(size[3]), (char *) malloc(size[3]) },
		{ (char *) malloc(size[4]), (char *) malloc(size[4]), (char *) malloc(size[4]), (char *) malloc(size[4]) },
		{ (char *) malloc(size[5]), (char *) malloc(size[5]), (char *) malloc(size[5]), (char *) malloc(size[5]) },
		{ (char *) malloc(size[6]), (char *) malloc(size[6]), (char *) malloc(size[6]), (char *) malloc(size[6]) }
	};



	for (i = 0; i < 7; i++) {
		random = rand();
		for (j = 0; j < cluster; j++) {
			ptr = (uint32 *) writeBuffer[i][j];
			for (k = 0; k < (size[i] / cluster); k++) {
				ptr[k] = random << 16 ^ rand();
			}
		}
	}

	freeSpace = maxSpace;
	usedData = 0;
	usedTime = 0;

	FILE * writeAction = NULL;
	writeAction = fopen(fileName,"w+");

	if (writeAction == NULL) {
		for (i = 0; i < 7; i++) {
			for (j = 0; j < cluster; j++) {
				free(writeBuffer[i][j]);
			}
		}
		return;
	}

	// write

	while (freeSpace > 512) {

		randSize = rand() % 7;
		randContent = rand() % cluster;

		gettimeofday(&start, NULL);
		outBytes = fwrite(writeBuffer[randSize][randContent],1,size[randSize],writeAction);
		gettimeofday(&end, NULL);

		freeSpace -= outBytes;
		usedData += outBytes;

		secs = end.tv_sec - start.tv_sec;
		usecs = end.tv_usec - start.tv_usec;
		mtime = ((secs) * 1000 + usecs / 1000.0) + 0.5;
		usedTime += mtime;

		if (outBytes == 0) break;
	}

	fclose(writeAction);

	// cleanup

	for (i = 0; i < 7; i++) {
		for (j = 0; j < cluster; j++) {
			free(writeBuffer[i][j]);
		}
	}

	// store

	sprintf(info, "Native SpeedTest(): write: data = %d time = %d", usedData, usedTime);
	LOG_I(info);
	writeSpeed = usedData / usedTime;

	// read

	freeSpace = maxSpace;
	usedData = 0;
	usedTime = 0;

	char * readBuffer = (char *) malloc(size[10]);
	FILE * readAction = NULL;
	readAction = fopen(fileName,"r");

	if (readAction == NULL) {
		free(readBuffer);
		return;
	}

	// read

	while (freeSpace > 512) {

		randSize = randSize = rand() % 11;
		randContent = rand() % ((freeSpace - 256) / (rand() % 8));

		gettimeofday(&start, NULL);
		fseek(readAction, randContent, SEEK_SET);
		inBytes = fread(readBuffer,1, size[randSize],readAction);
		gettimeofday(&end, NULL);

		freeSpace -= inBytes;
		usedData += inBytes;

		secs = end.tv_sec - start.tv_sec;
		usecs = end.tv_usec - start.tv_usec;
		mtime = ((secs) * 1000 + usecs / 1000.0) + 0.5;
		usedTime += mtime;

		if (inBytes == 0) break;
	}

	fclose(readAction);
	free(readBuffer);
	remove(fileName);

	// store

	sprintf(info, "Native SpeedTest(): read: data = %d time = %d", usedData, usedTime);
	LOG_I(info);
	readSpeed = usedData / usedTime;

	// set data

	env->SetDoubleField(modell, fields.readSpeed, readSpeed);
	env->SetDoubleField(modell, fields.writeSpeed, writeSpeed);
	env->SetIntField(modell, fields.usedTime, usedTime);
}


// runOneNative()

extern "C"
void Java_de_mehrmann_sdbooster_SpeedTest_runOneNative(JNIEnv * env, jobject thiz, jobject modell, jstring cardPath) {

	const char * path = (env)->GetStringUTFChars(cardPath, 0);
	const char * fileName = "test.0";
	const int size[11] = { 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576 };
	const int maxSpace = ((128 * 1024) * 1024);
	const int cluster = 4; //alignment
	const int readSize = 1024 * 1024;

	double readSpeed = 0.0;
	double writeSpeed = 0.0;
	int freeSpace = maxSpace;
	ssize_t usedData = 0;
	int readIndex;
	long usedTime = 0;

	struct timeval start, end;
	long mtime, secs, usecs;
	uint32 * ptr, random;

	char readBuffer[readSize]; // 1MB
	char info[256];
	char workDir[32];
	int i, j, k;
	int fd;
	int space, block;
	int randSize, randContent;

	ssize_t inBytes, outBytes;

	if (!dir_exist(path)) {
		return;
	} else {
		strcpy(workDir, path);
		strcat(workDir, "/");
		strcat(workDir, appTag);
		chdir(workDir);
	}

	// init

	void * writeBuffer[6][cluster] = {
		{ malloc(size[0]), malloc(size[0]), malloc(size[0]), malloc(size[0]) },
		{ malloc(size[1]), malloc(size[1]), malloc(size[1]), malloc(size[1]) },
		{ malloc(size[2]), malloc(size[2]), malloc(size[2]), malloc(size[2]) },
		{ malloc(size[3]), malloc(size[3]), malloc(size[3]), malloc(size[3]) },
		{ malloc(size[4]), malloc(size[4]), malloc(size[4]), malloc(size[4]) },
		{ malloc(size[5]), malloc(size[5]), malloc(size[5]), malloc(size[5]) }
	};

	random = rand();

	for (i = 0; i < 6; i++) {
		for (j = 0; j < cluster; j++) {
			ptr = (uint32 *) writeBuffer[i][j];
			for (k = 0; k < (size[i] / cluster); k++) {
				ptr[k] = random << 16 ^ rand();
			}
		}
	}

	fd = open(fileName, O_RDWR | O_CREAT | O_APPEND);

	// write

	while (freeSpace > 512) {
		randSize = rand() % 6;
		randContent = rand() % cluster;

		gettimeofday(&start, NULL);
		outBytes = write(fd, writeBuffer[randSize][randContent], size[randSize]-1);
		fsync(fd);
		gettimeofday(&end, NULL);

		secs  = end.tv_sec - start.tv_sec;
		usecs = end.tv_usec - start.tv_usec;
		mtime = ((secs) * 1000 + usecs/1000.0) + 0.5;
		usedTime += mtime;
		usedData += outBytes;

		freeSpace -= outBytes;
	}

	close(fd);

	// cleanup

	for (i = 0; i < 6; i++) {
		for (j = 0; j < cluster; j++) {
			free(writeBuffer[i][j]);
		}
	}

	// store

	sprintf(info, "Native SpeedTest(): write: data = %d time = %ld", usedData, usedTime);
	LOG_I(info);
	writeSpeed = usedData / usedTime;

	// read

	freeSpace = maxSpace;
	readIndex = usedData - 1;
	usedData = 0;
	usedTime = 0;

	fd = open(fileName, O_RDONLY);

	while (freeSpace > 512) {
		randSize = rand() % 11;

		gettimeofday(&start, NULL);
		inBytes = read(fd, &readBuffer, size[randSize]-1);
		gettimeofday(&end, NULL);

		if (inBytes > 0) {
			usedData += inBytes;
			freeSpace -= inBytes;

			secs  = end.tv_sec - start.tv_sec;
			usecs = end.tv_usec - start.tv_usec;
			mtime = ((secs) * 1000 + usecs/1000.0) + 0.5;
			usedTime += mtime;
		}
	}

	close(fd);

	// store

	sprintf(info, "Native SpeedTest(): read: data = %d time = %ld", usedData, usedTime);
	LOG_I(info);
	readSpeed = usedData / usedTime;

	// set data

	env->SetDoubleField(modell, fields.readSpeed, readSpeed);
	env->SetDoubleField(modell, fields.writeSpeed, writeSpeed);
	env->SetIntField(modell, fields.usedTime, usedTime);
}


// runNative()

extern "C"
void Java_de_mehrmann_sdbooster_SpeedTest_runNative(JNIEnv * env, jobject thiz, jobject modell, jstring cardPath) {

	const char * path = (env)->GetStringUTFChars(cardPath, 0);
	const int size = 4096;
	const int maxFiles = 128;
	const int cluster = 4;
	const int maxSpace = ((128 * 1024) * 1024);

	double readSpeed = 0.0;
	double writeSpeed = 0.0;
	int freeSpace = maxSpace;
	int usedFiles = 0;
	ssize_t usedData = 0;
	long usedTime = 0;

	struct timeval start, end;
	long mtime, secs, usecs;
	bool writeBack;
	uint32 * ptr, random;
	char file[32], number[32];
	char workDir[32];
	char info[256];
	char readBuffer[8192];
	int i, j;
	int fd;
	int space, block;

	ssize_t inBytes, outBytes;

	if (!dir_exist(path)) {
		return;
	} else {
		strcpy(workDir, path);
		strcat(workDir, "/");
		strcat(workDir, appTag);
		chdir(workDir);
	}

	// init

	void * buffer[cluster] = { malloc(size), malloc(size), malloc(size), malloc(size) };
	random = rand();

	for (i = 0; i < cluster; i++) {
		ptr = (uint32 *) buffer[i];
		for (j = 0; j < (size / cluster); j++) {
			ptr[j] = random << 16 ^ rand();
		}
	}

	// write

	while (usedFiles < maxFiles && freeSpace > 1024) {

		writeBack = false;
		space = rand() % (freeSpace / (rand() % (cluster * 8)));
		block = space < size ? space : size;

		char b[256];
		sprintf(b, "file write = %d", usedFiles);
		LOG_I(b);

		sprintf(number, "%d", usedFiles);
		strcpy(file, fileTag);
		strcat(file, number);
		fd = open(file, O_RDWR | O_CREAT | O_APPEND);

		gettimeofday(&start, NULL);

		for (j = 0; j < space; j += block) {
			outBytes = write(fd, buffer[rand() % cluster], block);

			if (outBytes > 0) {
				usedData += outBytes;
				writeBack = true;
			}
		}

		gettimeofday(&end, NULL);

		if (writeBack) {
			usedFiles++;

			secs  = end.tv_sec - start.tv_sec;
			usecs = end.tv_usec - start.tv_usec;
			mtime = ((secs) * 1000 + usecs/1000.0) + 0.5;
			usedTime += mtime;

			freeSpace -= space;

			char b[256];
			sprintf(b, "file write = %d was ok", usedFiles);
			LOG_I(b);
		}

		close(fd);

	}

	// store

	sprintf(info, "write: data = %d time = %ld", usedData, usedTime);
	LOG_I(info);
	writeSpeed = usedData / usedTime;


	for (i = 0; i < cluster; i++) free(buffer[i]);

	// read

	usedData = 0;
	usedTime = 0;

	for (i = 0; i < usedFiles - 1; i++) {

		char b[256];
		sprintf(b, "file read = %d", i);
		LOG_I(b);

		sprintf(number, "%d", i);
		strcpy(file, fileTag);
		strcat(file, number);
		fd = open(file, O_RDONLY);

		gettimeofday(&start, NULL);

		while ((inBytes = read(fd, &readBuffer, 8192)) > 0) {
			usedData += inBytes;
		}

		gettimeofday(&end, NULL);

		secs  = end.tv_sec - start.tv_sec;
		usecs = end.tv_usec - start.tv_usec;
		mtime = ((secs) * 1000 + usecs/1000.0) + 0.5;
		usedTime += mtime;

		close(fd);
	}

	// store

	sprintf(info, "read: data = %d time = %ld", usedData, usedTime);
	LOG_I(info);
	readSpeed = usedData / usedTime;

	remove_files(path, workDir, usedFiles);

	// set data

	env->SetDoubleField(modell, fields.readSpeed, readSpeed);
	env->SetDoubleField(modell, fields.writeSpeed, writeSpeed);
	env->SetIntField(modell, fields.usedTime, usedTime);
}

static bool find_fields(JNIEnv * env, field * fields, int count) {

	for (int i = 0; i < count; i++) {

        field * fPtr = &fields[i];

        jclass clazz = env->FindClass(fPtr->class_name);
        if (clazz == NULL) {
        	char buffer[32];
        	sprintf(buffer, "Can't find class %s", fPtr->class_name);
            LOG_E(buffer);
            return false;
        }

        jfieldID field = env->GetFieldID(clazz, fPtr->field_name, fPtr->field_type);
        if (field == NULL) {
        	char buffer[32];
        	sprintf(buffer, "Can't find member %s.%s", fPtr->class_name, fPtr->field_name);
            LOG_E(buffer);
            return false;
        }

        *(fPtr->jfield) = field;
    }

    return true;
}


static bool dir_exist(const char * path) {

   struct stat stats;

   if (chdir(path) == -1) {
	   LOG_D("chdir() failed");
	   return false;
   }

   if (stat(appTag, &stats) == -1) {
	   if (mkdir(appTag, 0777) == -1) {
		   LOG_D("mkdir() failed");
		   return false;
	   }
   } 
      
   return true;
}

static void remove_files(const char * path, const char * workDir, int usedFiles) {

	int i;
	struct stat stats;
	char file[32], number[32];

	if (dir_exist(path)) {
		chdir(workDir);

		for (i = 0; i < usedFiles; i++) {
			sprintf(number, "%d", i);
			strcpy(file, fileTag);
			strcat(file, number);

			if (stat(file, &stats) != -1) {
				remove(file);
			}
		}
	}
}
