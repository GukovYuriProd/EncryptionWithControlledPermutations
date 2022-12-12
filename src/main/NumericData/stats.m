%читаем первый файл
fileID = fopen('source.txt', 'r');
formatSpec = '%f';
sizeA = [1 Inf];
A = fscanf(fileID, formatSpec, sizeA);
A = A';
fclose(fileID);
%читаем второй файл
fileID1 = fopen('source1.txt', 'r');
formatSpec1 = '%f';
sizeB = [1 Inf];
B = fscanf(fileID1, formatSpec1, sizeB);
B = B';
fclose(fileID1);
figure(1);
histogram(A);
figure(2);
plot (A, B, 'k.');