/* Microsoft locking stuff */
static int lock_cb(void **mutexp, enum AVLockOp op) {
	HANDLE *mutex = (HANDLE *)mutexp;

	switch (op) {
	case AV_LOCK_CREATE:  ///< Create a mutex
		*mutex = CreateMutex(NULL, FALSE, NULL);
		if (*mutex) {
			return 0;
		}
		break;
	case AV_LOCK_OBTAIN:  ///< Lock the mutex
		if (WaitForSingleObject(*mutex, INFINITE) == 0)
			return 0;
		break;
	case AV_LOCK_RELEASE: ///< Unlock the mutex
		if (ReleaseMutex(*mutex))
			return 0;
		break;
	case AV_LOCK_DESTROY: ///< Free mutex resources
		if (CloseHandle(*mutex))
			return 0;
		break;
	}
	return -1;
}
