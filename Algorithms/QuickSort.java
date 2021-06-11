void QuickSort(int R[], int lo, int hi){
    int i = lo, j = hi;
    int temp;
    if(i < j){
        temp = R[i];
        while (i != j)
        {
            while(j > i && R[j] > temp)-- j;
            R[i] = R[j];
            while(i < j && R[i] < temp)++ i;
            R[j] = R[i];
        }
        R[i] = temp;
        QuickSort(R, lo, i - 1);
        QuickSort(R, i + 1, hi);
    }
}