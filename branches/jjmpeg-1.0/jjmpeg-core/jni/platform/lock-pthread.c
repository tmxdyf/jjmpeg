/* Pthread locking */

#include <pthread.h>

static int lock_cb(void **mutexp, enum AVLockOp op) {
	pthread_mutex_t **mutex = (pthread_mutex_t **)mutexp;

	switch (op) {
	case AV_LOCK_CREATE:  ///< Create a mutex
		*mutex = malloc(sizeof(pthread_mutex_t));
		if (*mutex) {
		pthread_mutex_init(*mutex, NULL);
		return 0;
		}
		break;
	case AV_LOCK_OBTAIN:  ///< Lock the mutex
		return pthread_mutex_lock(*mutex);
	case AV_LOCK_RELEASE: ///< Unlock the mutex
		return pthread_mutex_unlock(*mutex);
	case AV_LOCK_DESTROY: ///< Free mutex resources
		pthread_mutex_destroy(*mutex);
		free(*mutex);
		return 0;
	}
	return -1;
}
